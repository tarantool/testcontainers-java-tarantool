return{
    up = function()
        local space = box.schema.space.create('test_space', { if_not_exists = true })
        space:format({
            { name = 'bucket_id', type = 'unsigned', is_nullable = false },
            { name = 'test_id', type = 'integer' },
            { name = 'test_data', type = 'string' },
        })

        space:create_index('primary', { parts = { { field = 'test_id' } },
                                        unique = true,
                                        if_not_exists = true })

        space:create_index('bucket_id', {
            parts = { 'bucket_id' },
            unique = false,
            if_not_exists = true
        })

        require('migrator.utils').register_sharding_key('test_space', { 'test_id' })
        return true
    end
}
