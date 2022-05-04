return{

    up = function()
        local utils = require('migrator.utils')

        local routerSessionContexts = box.schema.space.create('spaceTest', { if_not_exists = true })
        routerSessionContexts:format({
            { name = 'sessionId', type = 'string' },
            { name = 'data', type = 'map', is_nullable = true },
            { name = 'ts', type = 'number' },

            -- vshard bucket id
            { name = 'bucket_id', type = 'unsigned', is_nullable = false },
        })

        routerSessionContexts:create_index('primary', { parts = { { field = 'sessionId' } },
                                                        unique = true,
                                                        if_not_exists = true })

        routerSessionContexts:create_index('bucket_id', {
            parts = { 'bucket_id' },
            unique = false,
            if_not_exists = true
        })

        utils.register_sharding_key('spaceTest', { 'sessionId' })

        return true


    end
}
