# Runbook: Memory Leak / OutOfMemoryError

## Symptoms
- Memory usage above 90% and climbing
- High CPU caused by garbage collection (GC overhead warnings)
- Logs contain "java.lang.OutOfMemoryError: Java heap space"
- Latency increases because of long GC pauses

## Diagnosis
1. Check the service metrics: memoryPercent above 90 is a strong signal.
2. Check the logs for OutOfMemoryError and GC overhead warnings.
3. Look for unbounded caches or collections growing without limit.

## Remediation
1. Restart the affected service to free memory. This is a temporary fix: the leak will return.
2. Capture a heap dump before restarting if possible, so developers can find the leak.
3. Open a ticket for the owning team to fix the leak (for example, add a maximum size to the cache).

## Escalation
If the service needs more than two restarts in 24 hours, escalate to the owning team's lead.