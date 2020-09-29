box.cfg {
    listen = 3301,
}

-- API user will be able to login with this password
box.schema.user.create('uuuser', { password = 'secret' })
-- API user will be able to create spaces, add or remove data, execute functions
box.schema.user.grant('uuuser', 'read,write,execute', 'universe')

function user_function_no_param()
    return 5;
end
