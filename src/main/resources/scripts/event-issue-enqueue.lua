local metaKey = KEYS[1]
local issuedUsersKey = KEYS[2]
local userRequestKey = KEYS[3]
local queueKey = KEYS[4]
local requestKey = KEYS[5]

local memberId = ARGV[1]
local requestId = ARGV[2]
local nowMs = tonumber(ARGV[3])
local queueLimit = tonumber(ARGV[4])
local statusTtlSeconds = tonumber(ARGV[5])

local startAtMs = tonumber(redis.call('HGET', metaKey, 'startAtEpochMs'))
if startAtMs ~= nil and nowMs < startAtMs then
    return {1, '', ''}
end

local endAtMs = tonumber(redis.call('HGET', metaKey, 'endAtEpochMs'))
if endAtMs ~= nil and nowMs >= endAtMs then
    return {5, '', ''}
end

if redis.call('SISMEMBER', issuedUsersKey, memberId) == 1 then
    return {2, '', ''}
end

local currentRequestId = redis.call('GET', userRequestKey)
if currentRequestId then
    local currentRequestKey = string.gsub(requestKey, requestId, currentRequestId)
    local currentStatus = redis.call('HGET', currentRequestKey, 'status')
    if currentStatus == 'SUCCEEDED' then
        return {2, currentRequestId, ''}
    end
    if currentStatus == 'WAITING' or currentStatus == 'PROCESSING' then
        local rank = redis.call('ZRANK', queueKey, currentRequestId)
        if rank then
            return {3, currentRequestId, tostring(rank + 1)}
        end
        return {3, currentRequestId, ''}
    end
end

local processingKey = string.gsub(queueKey, ':queue', ':processing')
local queueSize = redis.call('ZCARD', queueKey) + redis.call('ZCARD', processingKey)
if queueSize >= queueLimit then
    return {4, '', ''}
end

redis.call('HSET', requestKey,
    'memberId', memberId,
    'status', 'WAITING',
    'reason', '',
    'retryCount', '0',
    'createdAt', tostring(nowMs),
    'updatedAt', tostring(nowMs),
    'queueScore', tostring(nowMs),
    'couponId', '',
    'issuedAt', '',
    'expiredAt', ''
)
redis.call('EXPIRE', requestKey, statusTtlSeconds)
redis.call('SET', userRequestKey, requestId, 'EX', statusTtlSeconds)
redis.call('ZADD', queueKey, nowMs, requestId)

local newRank = redis.call('ZRANK', queueKey, requestId)
return {0, requestId, tostring(newRank + 1)}
