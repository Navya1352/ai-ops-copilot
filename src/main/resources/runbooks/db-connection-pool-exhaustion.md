# Runbook: Database Connection Pool Exhaustion

## Symptoms
- p99 latency very high (several seconds)
- Database connections used equal the maximum (for example 50/50)
- Logs contain "HikariPool - Connection is not available, request timed out"
- CPU and memory look normal: the service is waiting, not working hard

## Diagnosis
1. Check the service metrics: compare dbConnectionsUsed with dbConnectionsMax.
2. Check the logs for HikariPool timeout errors and slow query warnings.
3. Look for a slow query holding connections too long.

## Remediation
1. Restart the affected service to release stuck connections. This is safe and usually restores service within a minute.
2. If it happens again, increase the pool size (for checkout-service, raise maximum-pool-size from 50 to 80).
3. Ask the owning team to fix the slow query.

## Escalation
If latency stays above 2 seconds for 10 minutes after a restart, page the database on-call engineer.