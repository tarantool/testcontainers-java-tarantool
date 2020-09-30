local cartridge = require('cartridge')

local function init(opts) -- luacheck: no unused args
    -- if opts.is_master then
    -- end

    if opts.is_master then
        box.schema.user.grant('guest',
            'read,write',
            'universe',
            nil, { if_not_exists = true }
        )
    end

    local function get_replica_set()
        local vshard = require('vshard')
        local router_info, err = vshard.router.info()
        if err ~= nil then
            error(err)
        end

        local result = {}
        for i, v in pairs(router_info['replicasets']) do
            result[i] = v['master']
        end
        return result
    end

    local function get_fixed_replica_set()
        local replicaset = {}
        replicaset['fe2dc649-bd7a-458a-bbc4-917bb02a0cbd'] = {
            network_timeout = 0.5,
            status = 'available',
            uri = 'admin@127.0.0.1:53302',
            uuid = '399c5638-6266-4cbe-b67e-dc24b0c6e1d5'
        }
        replicaset['226f8e37-a5ce-44d4-ab60-7bbbb164cc81'] = {
            network_timeout = 0.5,
            status = 'available',
            uri = 'admin@127.0.0.1:53304',
            uuid = 'eb6f4fd5-7dc1-409c-99fe-6c2a49341491'
        }
        return replicaset
    end

    local function get_routers()
        local replicaset = {}
        replicaset['fe2dc649-bd7a-458a-bbc4-917bb02a0cbd'] = {
            network_timeout = 0.5,
            status = 'available',
            uri = 'admin@127.0.0.1:53301',
            uuid = '399c5638-6266-4cbe-b67e-111111111111'
        }
        replicaset['226f8e37-a5ce-44d4-ab60-7bbbb164cc81'] = {
            network_timeout = 0.5,
            status = 'available',
            uri = 'admin@127.0.0.1:53310',
            uuid = 'eb6f4fd5-7dc1-409c-99fe-6c2a49341410'
        }
        return replicaset
    end

    local httpd = cartridge.service_get('httpd')
    httpd:route({method = 'GET', path = '/hello'}, function()
        return {body = 'Hello world!'}
    end)

    local vshard = require('vshard')
    httpd:route({method = 'GET', path = '/endpoints'}, function(req)
        local json = require('json')
        local result = get_replica_set();

        return {body = json.encode(result)}
    end)

    local vshard = require('vshard')
    httpd:route({method = 'GET', path = '/routers'}, function(req)
        local json = require('json')
        local result = get_routers();

        return {body = json.encode(result)}
    end)

    httpd:route({method = 'GET', path = '/endpoints_fixed'}, function(req)
        local json = require('json')
        local result = get_fixed_replica_set();

        return {body = json.encode(result)}
    end)

    httpd:route({method = 'GET', path = '/endpoints_fixed'}, function(req)
        local json = require('json')
        local result = get_fixed_replica_set();

        return {body = json.encode(result)}
    end)

    httpd:route({method = 'GET', path = '/metadata'}, function(req)
        local json = require('json')
        local result, err = require('ddl').get_schema()

        return {body = json.encode(err)}
    end)

    rawset(_G, 'get_replica_set', get_replica_set)
    rawset(_G, 'get_routers', get_routers)
    rawset(_G, 'get_fixed_replica_set', get_fixed_replica_set)
    rawset(_G, 'get_fixed_replica_set', get_fixed_replica_set)

    return true
end

local function stop()
end

local function validate_config(conf_new, conf_old) -- luacheck: no unused args
    return true
end

local function apply_config(conf, opts) -- luacheck: no unused args
    -- if opts.is_master then
    -- end

    return true
end

return {
    role_name = 'app.roles.custom',
    init = init,
    stop = stop,
    validate_config = validate_config,
    apply_config = apply_config,
    dependencies = {'cartridge.roles.vshard-router'},
}
