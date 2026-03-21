local processingKey = KEYS[1]
local queueKey = KEYS[2]
local requestKeyPrefix = KEYS[3]

local nowMs = tonumber(ARGV[1])
local batchSize = tonumber(ARGV[2])

local requestIds = redis.call('ZRANGEBYSCORE', processingKey, '-inf', nowMs, 'LIMIT', 0, batchSize)
local moved = 0

for _, requestId in ipairs(requestIds) do
    if redis.call('ZREM', processingKey, requestId) == 1 then
        local requestKey = requestKeyPrefix .. requestId
        local queueScore = tonumber(redis.call('HGET', requestKey, 'queueScore'))
        if queueScore == nil then
            queueScore = nowMs
        end
        redis.call('ZADD', queueKey, queueScore, requestId)
        redis.call('HSET', requestKey,
            'status', 'WAITING',
            'updatedAt', tostring(nowMs)
        )
        moved = moved + 1
    end
end

return moved
