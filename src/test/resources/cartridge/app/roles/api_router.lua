local cartridge = require('cartridge')

local function profile_return(a, b, c, d, e, f)
    return a, b, c, d, e, f;
end

local function profile_insert(profile)
    local router = cartridge.service_get('vshard-router').get()

    local bucket_id = router:bucket_id_strcrc32(profile[1])
    profile = {profile[1], bucket_id, unpack(profile, 2)}

    return router:call(
        bucket_id,
        'write',
        'profile_storage_insert',
        {profile}
    )
end

local function profile_replace(profile)
    local router = cartridge.service_get('vshard-router').get()

    local bucket_id = router:bucket_id_strcrc32(profile[1])
    profile = {profile[1], bucket_id, unpack(profile, 2)}

    return router:call(
        bucket_id,
        'write',
        'profile_storage_replace',
        {profile}
    )
end

local function profile_update(profile_id, changes)
    local router = cartridge.service_get('vshard-router').get()
    local bucket_id = router:bucket_id_strcrc32(profile_id)
    
    return router:call(
		bucket_id,
        'write',
        'profile_storage_update',
        {profile_id, changes}
    )
end

local function profile_upsert(profile_id, tuple, changes)
    local router = cartridge.service_get('vshard-router').get()
    local bucket_id = router:bucket_id_strcrc32(profile_id)
    tuple = {tuple[1], bucket_id, unpack(tuple, 2)}
    
    return router:call(
		bucket_id,
        'write',
        'profile_storage_upsert',
        {profile_id, tuple, changes}
    )
end	


local function profile_get(profile_id)
    local router = cartridge.service_get('vshard-router').get()
    local bucket_id = router:bucket_id_strcrc32(profile_id)

    return router:call(
        bucket_id,
        'read',
        'profile_storage_get',
        {profile_id}
    )
end

local function profile_delete(profile_id)
    local router = cartridge.service_get('vshard-router').get()
    local bucket_id = router:bucket_id_strcrc32(profile_id)

    return router:call(
        bucket_id,
        'write',
        'profile_storage_delete',
        {profile_id}
    )
end

local function get_schema_metadata()
	local router = cartridge.service_get('vshard-router').get()
	local bucket_id = router:bucket_id_strcrc32(0)

	return router:callro(bucket_id, 'storage_get_space_format', {})
end

local function init(opts)
    if opts.is_master then
    end

	rawset(_G, 'profile_return', profile_return)

	rawset(_G, 'profile_insert', profile_insert)
    rawset(_G, 'profile_delete', profile_delete)
	rawset(_G, 'profile_replace', profile_replace)
    rawset(_G, 'profile_update', profile_update)
    rawset(_G, 'profile_upsert', profile_upsert)
    rawset(_G, 'profile_get', profile_get)

    return true
end

return {
    role_name = 'app.roles.api_router',
    init = init,
    dependencies = {
        'cartridge.roles.vshard-router'
    }
}
