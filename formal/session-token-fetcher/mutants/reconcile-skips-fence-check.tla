\* Code: reconcileTokenResponse drops `if (!isCurrentRuntime(context)) return
\* supersededFailure()` (line 332).
\* Expect: CacheIsCurrentWhenQuiet
Reconcile(c) ==
  /\ pc[c] = "reconcile"
  /\ cache' = IF resp[c].kind = "ok" THEN Freshest(cache, resp[c].tok) ELSE cache
  /\ OwnerResult(c, resp[c])
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp, cancelled,
                 nCancels>> /\ EnvUnchanged
