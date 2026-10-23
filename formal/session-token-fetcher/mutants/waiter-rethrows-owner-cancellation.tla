\* Code: awaitSharedTask rethrows the owner's CancellationException instead of
\* retrying (lines 229-233 replaced by `throw e`).
\* Expect: OnlyCancelledCallersSeeCancellation
WaiterRetry(c) ==
  /\ pc[c] = "waiter_retry"
  /\ res' = [res EXCEPT ![c] = Cancelled]
  /\ pc' = [pc EXCEPT ![c] = "done"]
  /\ UNCHANGED <<runtimeGeneration, sessionGeneration, task, deferreds, cache,
                 skip, ctx, myD, joined, attempt, seen, reqTok, resp, ores,
                 cancelled, nCancels>> /\ EnvUnchanged
