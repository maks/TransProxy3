#!/system/bin/sh

DIR=$2
type=$3
host=$4
port=$5
auth=$6
user=$7
pass=$8
arc=$9

PATH=$DIR:$PATH

case $1 in
 start)

echo "
base {
 log_debug = off;
 log_info = off;
 log = stderr;
 daemon = on;
 redirector = iptables;
}
" >$DIR/redsocks.conf

 case $type in
  http)
 case $auth in
  true)
  echo "
redsocks {
 local_ip = 127.0.0.1;
 local_port = 8123;
 ip = $host;
 port = $port;
 type = http-relay;
 login = \"$user\";
 password = \"$pass\";
} 
redsocks {
 local_ip = 127.0.0.1;
 local_port = 8124;
 ip = $host;
 port = $port;
 type = http-connect;
 login = \"$user\";
 password = \"$pass\";
} 
" >>$DIR/redsocks.conf
   ;;
   false)
   echo "
redsocks {
 local_ip = 127.0.0.1;
 local_port = 8123;
 ip = $host;
 port = $port;
 type = http-relay;
} 
redsocks {
 local_ip = 127.0.0.1;
 local_port = 8124;
 ip = $host;
 port = $port;
 type = http-connect;
} 
 " >>$DIR/redsocks.conf
   ;;
 esac
   ;;
  socks)
  echo "
redsocks {
 local_ip = 127.0.0.1;
 local_port = 8123;
 ip = $host;
 port = $port;
 type = socks5;
 }
 " >>$DIR/redsocks.conf
   ;;
 esac

 $DIR/redsocks-$arch -p $DIR/redsocks.pid -c $DIR/redsocks.conf
 ;;
stop)
  kill `cat $DIR/redsocks.pid`
  
  rm $DIR/redsocks.pid
  
  rm $DIR/redsocks.conf
esac
