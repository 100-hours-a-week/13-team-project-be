local queueKey = KEYS[1]
local processingKey = KEYS[2]
local requestKeyPrefix = KEYS[3]

local nowMs = tonumber(ARGV[1])
local leaseExpireAtMs = tonumber(ARGV[2])

local values = redis.call('ZRANGE', queueKey, 0, 0)
if #values == 0 then
    return {}
end

local requestId = values[1]
if redis.call('ZREM', queueKey, requestId) == 0 then
    return {}
end

local requestKey = requestKeyPrefix .. requestId
local memberId = redis.call('HGET', requestKey, 'memberId')
if not memberId then
    return {}
end

redis.call('ZADD', processingKey, leaseExpireAtMs, requestId)
redis.call('HSET', requestKey,
    'status', 'PROCESSING',
    'reason', '',
    'updatedAt', tostring(nowMs)
)

return {requestId, memberId}
