\* Code: the owner's catch (CancellationException) drops `deferred.cancel(e)`
\* (line 210), so waiters on that deferred never wake.
\* Expect: EveryCallReturns
CancelDeferred(c) ==
  /\ pc[c] = "cancel_deferred"
  /\ pc' = [pc EXCEPT ![c] = "cancel_finally"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp, ores,
                 res, cancelled, nCancels>> /\ EnvUnchanged
