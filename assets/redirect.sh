#!/system/bin/sh
case $1 in
start)
   iptables -t nat -A OUTPUT -p tcp --dport 80 -j REDIRECT --to 8123
   iptables -t nat -A OUTPUT -p tcp --dport 443 -j REDIRECT --to 8124
   iptables -t nat -A OUTPUT -p tcp --dport 5228 -j REDIRECT --to 8124
 ;;
stop)
 iptables -t nat -F OUTPUT
 ;;
esac