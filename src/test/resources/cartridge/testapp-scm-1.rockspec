package = 'testapp'
version = 'scm-1'
source  = {
    url = '/dev/null',
}
-- Put any modules your app depends on here
dependencies = {
    'tarantool',
    'lua >= 5.1',
    -- Be careful when updating to 2.7.4. Tarantool doesn't bind ports before roles are configured.
    'cartridge == 2.7.3-1',
    'crud == 0.11.1-1',
    'migrations == 0.4.2-1',
}
build = {
    type = 'none';
}
