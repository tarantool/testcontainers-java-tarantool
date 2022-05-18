
local function init_space()
    local profile = box.schema.space.create(
        'profile', -- имя спейса для хранения профилей
        {
            -- формат хранимых кортежей
            format = {
                {'profile_id', 'unsigned'},
                {'bucket_id', 'unsigned'},
                {'fio', 'string'},
                {'age', 'unsigned'},
                {'balance', 'unsigned', is_nullable = true}
            },

            -- создадим спейс, только если его не было
            if_not_exists = true,
        }
    )

    -- создадим индекс по id профиля
    profile:create_index('profile_id', {
        parts = {'profile_id'},
        if_not_exists = true,
    })

    profile:create_index('bucket_id', {
        parts = {'bucket_id'},
        unique = false,
        if_not_exists = true,
    })
end

local function profile_storage_select(id)
    return box.space.profile:select(id)
end

local function profile_storage_get(id)
    return box.space.profile.index.profile_id:get(id)
end

local function profile_storage_insert(tuple)
    return box.space.profile:insert(tuple)
end

local function profile_storage_update(id, changes)
    return box.space.profile:update(id, changes)
end

local function profile_storage_upsert(id, tuple, changes)
    return box.space.profile:upset(id, tuple, changes)
end

local function profile_storage_replace(tuple)
    return box.space.profile:replace(tuple)
end

local function profile_storage_delete(id)
    return box.space.profile:delete(id)
end

local function storage_get_space_format()
    local ddl = require('ddl')
    return ddl.get_schema()
end

local function init(opts)
    if opts.is_master then
        init_space()

        box.schema.func.create('profile_storage_insert', {if_not_exists = true})
        box.schema.func.create('profile_storage_get', {if_not_exists = true})
        box.schema.func.create('profile_storage_update', {if_not_exists = true})
        box.schema.func.create('profile_storage_upsert', {if_not_exists = true})
        box.schema.func.create('profile_storage_replace', {if_not_exists = true})
        box.schema.func.create('profile_storage_delete', {if_not_exists = true})

        box.schema.func.create('storage_get_space_format', {if_not_exists = true})
    end


    rawset(_G, 'profile_storage_insert', profile_storage_insert)
    rawset(_G, 'profile_storage_get', profile_storage_get)
    rawset(_G, 'profile_storage_update', profile_storage_update)
    rawset(_G, 'profile_storage_upsert', profile_storage_upsert)
    rawset(_G, 'profile_storage_replace', profile_storage_replace)
    rawset(_G, 'profile_storage_delete', profile_storage_delete)

    rawset(_G, 'storage_get_space_format', storage_get_space_format)

    return true
end

return {
    role_name = 'app.roles.api_storage',
    init = init,
    utils = {
        profile_storage_get = profile_storage_get,
        profile_storage_insert = profile_storage_insert,
        profile_storage_update = profile_storage_update,
        profile_storage_upsert = profile_storage_upsert,
        profile_storage_replace = profile_storage_replace,
        profile_storage_delete = profile_storage_delete,

        storage_get_space_format = storage_get_space_format,
    },
    dependencies = {
        'cartridge.roles.vshard-storage'
    }
}
