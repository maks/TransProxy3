#!/system/bin/sh
case $1 in
start)
 case $2 in
  http)
   iptables -t nat -A OUTPUT -p tcp --dport 80 -j REDIRECT --to 8123
   iptables -t nat -A OUTPUT -p tcp --dport 443 -j REDIRECT --to 8124
   iptables -t nat -A OUTPUT -p tcp --dport 5228 -j REDIRECT --to 8124
   ;;
  socks)
   iptables -t nat -A OUTPUT -p tcp -j REDIRECT --to 8123
 esac
 ;;
stop)
 iptables -t nat -F OUTPUT
 ;;
esac