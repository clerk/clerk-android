--------------------------- MODULE SessionTokenFetcher ---------------------------
(***************************************************************************)
(* Model of request deduplication and generation fencing in                *)
(* source/api/src/main/kotlin/com/clerk/api/session/SessionTokenFetcher.kt *)
(*                                                                         *)
(* One session, one cache key (no template). Each step is one indivisible  *)
(* step in the code: a synchronized(runtimeLock) block, one                *)
(* ConcurrentHashMap or SessionTokensCache call, or one                    *)
(* CompletableDeferred transition. Line numbers are at the sync commit in  *)
(* .claude/skills/session-token-fetcher-model/SKILL.md.                    *)
(***************************************************************************)
EXTENDS Integers, FiniteSets, TLC

CONSTANTS Callers, MaxResets, MaxInvalidations, MaxCancels, NoTask

NoToken == [gen |-> <<-1, -1>>]

Ok(t) == [kind |-> "ok", tok |-> t]
Superseded == [kind |-> "superseded", tok |-> NoToken]
Failed == [kind |-> "failed", tok |-> NoToken]
Cancelled == [kind |-> "cancelled", tok |-> NoToken]
Unset == [kind |-> "unset", tok |-> NoToken]

VARIABLES
  runtimeGeneration,  \* runtimeGeneration, line 73
  sessionGeneration,  \* sessionGenerations[session.id], line 74
  task,               \* tokenTasks[cacheKey], line 70
  deferreds,          \* each CompletableDeferred: [st, val]
  cache,              \* SessionTokensCache entry for cacheKey
  pc, skip, ctx, myD, joined, attempt, seen, reqTok, resp, ores, res, cancelled,
  resetPc, resetRelease, invPc, invRelease, nResets, nInvalidations, nCancels

vars == <<runtimeGeneration, sessionGeneration, task, deferreds, cache,
          pc, skip, ctx, myD, joined, attempt, seen, reqTok, resp, ores, res,
          cancelled, resetPc, resetRelease, invPc, invRelease, nResets,
          nInvalidations, nCancels>>

Gen == <<runtimeGeneration, sessionGeneration>>

\* isCurrentRuntime(context), lines 243-247
Current(c) == ctx[c] = Gen

\* SessionTokensCache.storeIfFresher. Tokens are tagged only with their
\* generation, so which of two same-generation tokens wins does not matter here.
Freshest(old, new) == new

Init ==
  /\ runtimeGeneration = 0
  /\ sessionGeneration = 0
  /\ task = NoTask
  /\ deferreds = [d \in {} |-> 0]
  /\ cache = NoToken
  /\ pc = [c \in Callers |-> "start"]
  \* GetTokenOptions.skipCache: one caller may skip the cache, the rest share.
  /\ skip \in {f \in [Callers -> BOOLEAN] : \A x, y \in Callers : f[x] /\ f[y] => x = y}
  /\ ctx = [c \in Callers |-> <<-1, -1>>]
  /\ myD = [c \in Callers |-> NoTask]
  /\ joined = [c \in Callers |-> NoTask]
  /\ attempt = [c \in Callers |-> 0]
  /\ seen = [c \in Callers |-> NoToken]
  /\ reqTok = [c \in Callers |-> NoToken]
  /\ resp = [c \in Callers |-> Unset]
  /\ ores = [c \in Callers |-> Unset]
  /\ res = [c \in Callers |-> Unset]
  /\ cancelled = [c \in Callers |-> FALSE]
  /\ resetPc = "idle" /\ resetRelease = NoTask
  /\ invPc = "idle" /\ invRelease = NoTask
  /\ nResets = 0 /\ nInvalidations = 0 /\ nCancels = 0

EnvUnchanged == UNCHANGED <<resetPc, resetRelease, invPc, invRelease, nResets,
                            nInvalidations>>

\* Owner hands its result on: skipCache returns it (line 195); otherwise it
\* completes the shared deferred first (line 207).
OwnerResult(c, r) ==
  IF skip[c]
  THEN /\ res' = [res EXCEPT ![c] = r] /\ pc' = [pc EXCEPT ![c] = "done"]
       /\ UNCHANGED ores
  ELSE /\ ores' = [ores EXCEPT ![c] = r] /\ pc' = [pc EXCEPT ![c] = "complete"]
       /\ UNCHANGED res

-----------------------------------------------------------------------------
(* Caller steps *)

\* getTokenResult, lines 161-172: capture the FetchContext under the lock.
Capture(c) ==
  /\ pc[c] = "start"
  /\ ctx' = [ctx EXCEPT ![c] = Gen]
  /\ pc' = [pc EXCEPT ![c] = IF skip[c] THEN "fetch_check" ELSE "register"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds, cache,
                 skip, myD, joined, attempt, seen, reqTok, resp, ores, res,
                 cancelled, nCancels>> /\ EnvUnchanged

\* fetchTokenWithDeduplication, lines 197-204: check, then putIfAbsent.
Register(c) ==
  /\ pc[c] = "register"
  /\ LET d == <<c, attempt[c]>> IN
     IF ~Current(c)
     THEN /\ res' = [res EXCEPT ![c] = Superseded]
          /\ pc' = [pc EXCEPT ![c] = "done"]
          /\ UNCHANGED <<task, deferreds, myD, joined>>
     ELSE IF task = NoTask
     THEN /\ task' = d
          /\ deferreds' = deferreds @@ (d :> [st |-> "pending", val |-> Unset])
          /\ myD' = [myD EXCEPT ![c] = d]
          /\ pc' = [pc EXCEPT ![c] = "fetch_check"]
          /\ UNCHANGED <<res, joined>>
     ELSE /\ joined' = [joined EXCEPT ![c] = task]
          /\ pc' = [pc EXCEPT ![c] = "await"]
          /\ UNCHANGED <<res, task, deferreds, myD>>
  /\ attempt' = [attempt EXCEPT ![c] = @ + 1]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, cache, skip, ctx,
                 seen, reqTok, resp, ores, cancelled, nCancels>> /\ EnvUnchanged

\* fetchToken, line 253.
FetchCheck(c) ==
  /\ pc[c] = "fetch_check"
  /\ IF ~Current(c)
     THEN OwnerResult(c, Superseded)
     ELSE /\ pc' = [pc EXCEPT ![c] = "read_cache"] /\ UNCHANGED <<res, ores>>
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp,
                 cancelled, nCancels>> /\ EnvUnchanged

\* fetchToken, lines 270-281: read the cache outside the lock. Whether the
\* cached token is still valid (isTokenValid) is a free choice.
ReadCache(c) ==
  /\ pc[c] = "read_cache"
  /\ \E valid \in BOOLEAN :
       seen' = [seen EXCEPT ![c] =
                  IF skip[c] \/ cache = NoToken \/ ~valid THEN NoToken ELSE cache]
  /\ pc' = [pc EXCEPT ![c] = "post_check"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds, cache,
                 skip, ctx, myD, joined, attempt, reqTok, resp, ores, res,
                 cancelled, nCancels>> /\ EnvUnchanged

\* fetchToken, lines 283-291. A request is tagged with the caller's
\* generation; the server's token carries that tag.
PostCheck(c) ==
  /\ pc[c] = "post_check"
  /\ IF ~Current(c)
     THEN OwnerResult(c, Superseded) /\ UNCHANGED reqTok
     ELSE IF seen[c] # NoToken
     THEN OwnerResult(c, Ok(seen[c])) /\ UNCHANGED reqTok
     ELSE /\ reqTok' = [reqTok EXCEPT ![c] = [gen |-> ctx[c]]]
          /\ pc' = [pc EXCEPT ![c] = "in_flight"]
          /\ UNCHANGED <<res, ores>>
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, resp, cancelled,
                 nCancels>> /\ EnvUnchanged

\* requestToken returns, success or failure (line 290).
Respond(c) ==
  /\ pc[c] = "in_flight"
  /\ \E ok \in BOOLEAN : resp' = [resp EXCEPT ![c] = IF ok THEN Ok(reqTok[c]) ELSE Failed]
  /\ pc' = [pc EXCEPT ![c] = "reconcile"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, reqTok, ores, res,
                 cancelled, nCancels>> /\ EnvUnchanged

\* reconcileTokenResponse, lines 330-355.
Reconcile(c) ==
  /\ pc[c] = "reconcile"
  /\ IF ~Current(c)
     THEN OwnerResult(c, Superseded) /\ UNCHANGED cache
     ELSE /\ cache' = IF resp[c].kind = "ok" THEN Freshest(cache, resp[c].tok) ELSE cache
          /\ OwnerResult(c, resp[c])
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp, cancelled,
                 nCancels>> /\ EnvUnchanged

\* deferred.complete(it), line 207. No-op if reset or invalidateSession
\* already completed it.
Complete(c) ==
  /\ pc[c] = "complete"
  /\ deferreds' = IF deferreds[myD[c]].st = "pending"
                  THEN [deferreds EXCEPT ![myD[c]] = [st |-> "done", val |-> ores[c]]]
                  ELSE deferreds
  /\ pc' = [pc EXCEPT ![c] = "finally"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, cache, skip,
                 ctx, myD, joined, attempt, seen, reqTok, resp, ores, res,
                 cancelled, nCancels>> /\ EnvUnchanged

\* finally { tokenTasks.remove(cacheKey, deferred) }, line 216.
Finally(c) ==
  /\ pc[c] = "finally"
  /\ task' = IF task = myD[c] THEN NoTask ELSE task
  /\ res' = [res EXCEPT ![c] = ores[c]]
  /\ pc' = [pc EXCEPT ![c] = "done"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp, ores,
                 cancelled, nCancels>> /\ EnvUnchanged

\* The owner is cancelled while suspended in requestToken (the only
\* suspension point in fetchToken).
CancelOwner(c) ==
  /\ pc[c] = "in_flight"
  /\ nCancels < MaxCancels
  /\ nCancels' = nCancels + 1
  /\ cancelled' = [cancelled EXCEPT ![c] = TRUE]
  /\ IF skip[c]
     THEN /\ res' = [res EXCEPT ![c] = Cancelled] /\ pc' = [pc EXCEPT ![c] = "done"]
     ELSE /\ pc' = [pc EXCEPT ![c] = "cancel_remove"] /\ UNCHANGED res
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp, ores>>
  /\ EnvUnchanged

\* catch (CancellationException): tokenTasks.remove, line 209.
CancelRemove(c) ==
  /\ pc[c] = "cancel_remove"
  /\ task' = IF task = myD[c] THEN NoTask ELSE task
  /\ pc' = [pc EXCEPT ![c] = "cancel_deferred"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp, ores, res,
                 cancelled, nCancels>> /\ EnvUnchanged

\* deferred.cancel(e), line 210; then finally (line 216) and rethrow.
CancelDeferred(c) ==
  /\ pc[c] = "cancel_deferred"
  /\ deferreds' = IF deferreds[myD[c]].st = "pending"
                  THEN [deferreds EXCEPT ![myD[c]] = [st |-> "cancelled", val |-> Unset]]
                  ELSE deferreds
  /\ pc' = [pc EXCEPT ![c] = "cancel_finally"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, cache, skip,
                 ctx, myD, joined, attempt, seen, reqTok, resp, ores, res,
                 cancelled, nCancels>> /\ EnvUnchanged

CancelFinally(c) ==
  /\ pc[c] = "cancel_finally"
  /\ task' = IF task = myD[c] THEN NoTask ELSE task
  /\ res' = [res EXCEPT ![c] = Cancelled]
  /\ pc' = [pc EXCEPT ![c] = "done"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp, ores,
                 cancelled, nCancels>> /\ EnvUnchanged

\* awaitSharedTask: task.await() returns (line 228) or throws because the
\* owner cancelled (line 229).
Await(c) ==
  /\ pc[c] = "await"
  /\ deferreds[joined[c]].st # "pending"
  /\ IF deferreds[joined[c]].st = "done"
     THEN /\ ores' = [ores EXCEPT ![c] = deferreds[joined[c]].val]
          /\ pc' = [pc EXCEPT ![c] = "waiter_check"]
     ELSE /\ pc' = [pc EXCEPT ![c] = "waiter_retry"] /\ UNCHANGED ores
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp, res,
                 cancelled, nCancels>> /\ EnvUnchanged

\* The waiter itself is cancelled while awaiting.
CancelWaiter(c) ==
  /\ pc[c] = "await"
  /\ nCancels < MaxCancels
  /\ nCancels' = nCancels + 1
  /\ cancelled' = [cancelled EXCEPT ![c] = TRUE]
  /\ res' = [res EXCEPT ![c] = Cancelled]
  /\ pc' = [pc EXCEPT ![c] = "done"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp, ores>>
  /\ EnvUnchanged

\* Lines 230-233 then 204: remove the dead task and retry with the same context.
WaiterRetry(c) ==
  /\ pc[c] = "waiter_retry"
  /\ task' = IF task = joined[c] THEN NoTask ELSE task
  /\ pc' = [pc EXCEPT ![c] = "register"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp, ores, res,
                 cancelled, nCancels>> /\ EnvUnchanged

\* supersededIfInvalidatedSince, lines 235-241.
WaiterCheck(c) ==
  /\ pc[c] = "waiter_check"
  /\ res' = [res EXCEPT ![c] = IF Current(c) THEN ores[c] ELSE Superseded]
  /\ pc' = [pc EXCEPT ![c] = "done"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp, ores,
                 cancelled, nCancels>> /\ EnvUnchanged

-----------------------------------------------------------------------------
(* Fences. Both callers of reset() (Clerk.reset, Clerk.kt:895-896, and      *)
(* resetClientStateForDeviceTokenChange, ConfigurationManager.kt:890-891)    *)
(* call SessionTokenFetcher.reset() and then SessionTokensCache.clear().    *)

CallerVarsUnchanged ==
  UNCHANGED <<pc, skip, ctx, myD, joined, attempt, seen, reqTok, resp,
              ores, res, cancelled, nCancels>>

\* reset(), lines 81-86: bump and take the tasks under the lock.
ResetFetcher ==
  /\ resetPc = "idle" /\ nResets < MaxResets
  /\ nResets' = nResets + 1
  /\ runtimeGeneration' = runtimeGeneration + 1
  /\ sessionGeneration' = 0
  /\ resetRelease' = task
  /\ task' = NoTask
  /\ resetPc' = "release"
  /\ UNCHANGED <<deferreds, cache, invPc, invRelease, nInvalidations>>
  /\ CallerVarsUnchanged

\* reset(), line 87: complete the taken tasks outside the lock.
ResetRelease ==
  /\ resetPc = "release"
  /\ deferreds' = IF resetRelease # NoTask /\ deferreds[resetRelease].st = "pending"
                  THEN [deferreds EXCEPT ![resetRelease] = [st |-> "done", val |-> Superseded]]
                  ELSE deferreds
  /\ resetPc' = "clear"
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, cache, resetRelease,
                 invPc, invRelease, nResets, nInvalidations>>
  /\ CallerVarsUnchanged

\* SessionTokensCache.clear(), the statement after reset() in both callers.
ClearCache ==
  /\ resetPc = "clear"
  /\ cache' = NoToken
  /\ resetPc' = "idle"
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds,
                 resetRelease, invPc, invRelease, nResets, nInvalidations>>
  /\ CallerVarsUnchanged

\* invalidateSession(sessionId), lines 94-101: one locked block.
Invalidate ==
  /\ invPc = "idle" /\ nInvalidations < MaxInvalidations
  /\ nInvalidations' = nInvalidations + 1
  /\ sessionGeneration' = sessionGeneration + 1
  /\ cache' = NoToken
  /\ invRelease' = task
  /\ task' = NoTask
  /\ invPc' = "release"
  /\ UNCHANGED <<runtimeGeneration, deferreds, resetPc, resetRelease, nResets>>
  /\ CallerVarsUnchanged

\* invalidateSession, line 102.
InvalidateRelease ==
  /\ invPc = "release"
  /\ deferreds' = IF invRelease # NoTask /\ deferreds[invRelease].st = "pending"
                  THEN [deferreds EXCEPT ![invRelease] = [st |-> "done", val |-> Superseded]]
                  ELSE deferreds
  /\ invPc' = "idle"
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, cache, invRelease,
                 resetPc, resetRelease, nResets, nInvalidations>>
  /\ CallerVarsUnchanged

-----------------------------------------------------------------------------

CallerStep(c) ==
  \/ Capture(c) \/ Register(c) \/ FetchCheck(c) \/ ReadCache(c) \/ PostCheck(c)
  \/ Respond(c) \/ Reconcile(c) \/ Complete(c) \/ Finally(c)
  \/ CancelRemove(c) \/ CancelDeferred(c) \/ CancelFinally(c)
  \/ Await(c) \/ WaiterRetry(c) \/ WaiterCheck(c)

AllDone == \A c \in Callers : pc[c] = "done"
Quiet == resetPc = "idle" /\ invPc = "idle"

Next ==
  \/ \E c \in Callers : CallerStep(c) \/ CancelOwner(c) \/ CancelWaiter(c)
  \/ ResetFetcher \/ ResetRelease \/ ClearCache \/ Invalidate \/ InvalidateRelease
  \/ (AllDone /\ Quiet /\ UNCHANGED vars)

\* Callers and fences make progress; cancellations and new fences are optional.
Spec ==
  /\ Init /\ [][Next]_vars
  /\ \A c \in Callers : WF_vars(CallerStep(c))
  /\ WF_vars(ResetRelease) /\ WF_vars(ClearCache) /\ WF_vars(InvalidateRelease)

-----------------------------------------------------------------------------
(* Properties *)

\* A caller only receives a token minted for the generation it started in.
\* Harm if broken: a caller that started after sign-in state was reset, or
\* after biometric reverification, gets a token from before it.
NoTokenFromAnotherGeneration ==
  \A c \in Callers : (pc[c] = "done" /\ res[c].kind = "ok") => res[c].tok.gen = ctx[c]

\* A caller sees a cancellation only if it was cancelled itself.
\* Harm if broken: a waiter's coroutine dies because another caller's did.
OnlyCancelledCallersSeeCancellation ==
  \A c \in Callers : (pc[c] = "done" /\ res[c].kind = "cancelled") => cancelled[c]

\* Once no fence is in progress, the cache holds no token from an older
\* generation. Harm if broken: a later getToken serves a stale token.
CacheIsCurrentWhenQuiet ==
  Quiet => (cache = NoToken \/ cache.gen = Gen)

\* Deduplication: at most one non-skipCache network request per generation.
AtMostOneSharedRequest ==
  \A c1, c2 \in Callers :
    (/\ pc[c1] = "in_flight" /\ pc[c2] = "in_flight"
     /\ ~skip[c1] /\ ~skip[c2] /\ ctx[c1] = ctx[c2]) => c1 = c2

\* Every getToken call returns.
EveryCallReturns == <>AllDone

=============================================================================
