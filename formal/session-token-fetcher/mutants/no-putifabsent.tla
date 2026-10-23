\* Code: `tokenTasks[context.cacheKey] = deferred` replaces putIfAbsent (line 201),
\* so every caller becomes an owner.
\* Expect: AtMostOneSharedRequest
Register(c) ==
  /\ pc[c] = "register"
  /\ LET d == <<c, attempt[c]>> IN
     IF ~Current(c)
     THEN /\ res' = [res EXCEPT ![c] = Superseded]
          /\ pc' = [pc EXCEPT ![c] = "done"]
          /\ UNCHANGED <<task, deferreds, myD, joined>>
     ELSE /\ task' = d
          /\ deferreds' = deferreds @@ (d :> [st |-> "pending", val |-> Unset])
          /\ myD' = [myD EXCEPT ![c] = d]
          /\ pc' = [pc EXCEPT ![c] = "fetch_check"]
          /\ UNCHANGED <<res, joined>>
  /\ attempt' = [attempt EXCEPT ![c] = @ + 1]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, cache, skip, ctx,
                 seen, reqTok, resp, ores, cancelled, nCancels>> /\ EnvUnchanged
