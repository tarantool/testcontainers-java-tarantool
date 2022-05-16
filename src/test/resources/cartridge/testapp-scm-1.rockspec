package = 'testapp'
version = 'scm-1'
source  = {
    url = '/dev/null',
}
dependencies = {
    'tarantool',
    'lua >= 5.1',
    'cartridge == 2.7.4-1',
    'crud == 0.11.1-1',
    'migrations == 0.4.2-1',
}
build = {
    type = 'none';
}
