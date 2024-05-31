local inputString = KEYS[2]
local actualKey = inputString
local colonIndex = string.find(actualKey, ":")
if colonIndex ~= nil then
    actualKey = string.sub(actualKey, colonIndex + 1)
end

local jsonArrayStr = ARGV[1]
local jsonArray = cjson.decode(jsonArrayStr)

local result = {}
local tokenIsNull = false
local tokenIsNullSeatTypeCounts = {}

for index, jsonObj in ipairs(jsonArray) do
    local seatType = tonumber(jsonObj.seatType)
    local count = tonumber(jsonObj.count)
    local actualInnerHashKey = actualKey .. "_" .. seatType
    local ticketSeatAvailabilityTokenValue = tonumber(redis.call('hget', KEYS[1], tostring(actualInnerHashKey)))
    if ticketSeatAvailabilityTokenValue < count then
        tokenIsNull = true
        --tokenIsNullSeatTypeCounts中插入令牌不够的座位类型和需要的数量
        table.insert(tokenIsNullSeatTypeCounts, seatType .. "_" .. count)
    end
end

result['tokenIsNull'] = tokenIsNull
if tokenIsNull then
    --如果令牌数量不够，则直接返回了
    result['tokenIsNullSeatTypeCounts'] = tokenIsNullSeatTypeCounts
    return cjson.encode(result)
end

--令牌数量够，那么开始扣减受影响的站点的令牌
local alongJsonArrayStr = ARGV[2]
local alongJsonArray = cjson.decode(alongJsonArrayStr)

for index, jsonObj in ipairs(jsonArray) do
    local seatType = tonumber(jsonObj.seatType)
    local count = tonumber(jsonObj.count)
    for indexTwo, alongJsonObj in ipairs(alongJsonArray) do
        local startStation = tostring(alongJsonObj.startStation)
        local endStation = tostring(alongJsonObj.endStation)
        local actualInnerHashKey = startStation .. "_" .. endStation .. "_" .. seatType
        redis.call('hincrby', KEYS[1], tostring(actualInnerHashKey), -count)
    end
end

return cjson.encode(result)
