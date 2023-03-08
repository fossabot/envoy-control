function envoy_on_request(handle)
    local autoServiceTagPreference = handle:metadata():get("auto_service_tag_preference")
    if autoServiceTagPreference ~= nil then
        local serviceTagMetadataKey = handle:metadata():get("service_tag_metadata_key")
        if serviceTagMetadataKey ~= nil then
            local requestServiceTag = (handle:streamInfo():dynamicMetadata():get("envoy.lb") or {})[serviceTagMetadataKey]
            if (requestServiceTag ~= nil) then
                for i=1,#autoServiceTagPreference do
                    if requestServiceTag == autoServiceTagPreference[i] then
                        handle:respond(
                                {
                                    [":status"] = "400"
                                },
                                "nooope"
                        )
                    end
                end
            end
        end
    end
end


function debug(handle)
    local autoServiceTagPreference = handle:metadata():get("auto_service_tag_preference")

    if autoServiceTagPreference ~= nil and next(autoServiceTagPreference) ~= nil then
        local joined = table.concat(autoServiceTagPreference, "|")
        handle:logInfo("auto_service_tag_preference is not empty!: "..joined)
    else
        handle:logInfo("auto_service_tag_preference is empty!")
    end

    local requestServiceTag = handle:headers():get("x-service-tag")
    if requestServiceTag ~= nil then
        handle:logInfo("request x-service-tag: "..requestServiceTag)
    else
        handle:logInfo("request x-service-tag is nil")
    end

    local serviceTagMetadataKey = handle:metadata():get("service_tag_metadata_key")
    if serviceTagMetadataKey ~= nil then
        handle:logInfo("serviceTagMetadataKey: "..serviceTagMetadataKey)
        local metadataServiceTag = (handle:streamInfo():dynamicMetadata():get("envoy.lb") or {})[serviceTagMetadataKey]
        if (metadataServiceTag ~= nil) then
            handle:logInfo("metadata service tag: "..metadataServiceTag)
        else
            handle:logInfo("metadata service tag is nil")
        end

    else
        handle:logInfo("serviceTagMetadataKey is nil")
    end
end
