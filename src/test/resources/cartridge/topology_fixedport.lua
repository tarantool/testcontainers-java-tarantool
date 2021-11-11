cartridge = require('cartridge')
replicasets = {{
    alias = 'router-fixedport',
    roles = {'vshard-router', 'app.roles.custom', 'app.roles.api_router'},
    join_servers = {{uri = 'localhost:13301'}}
}, {
    alias = 'storage-fixedport',
    roles = {'vshard-storage', 'app.roles.api_storage'},
    join_servers = {{uri = 'localhost:13302'}}
}}
return cartridge.admin_edit_topology({replicasets = replicasets})
