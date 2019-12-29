Create using command line
===========================
1. Create keystore:
keytool -keystore <path to keystore directory>\<keystore file> -alias jetty -genkey -keyalg RSA
Follow the instructions. Remember passwords, you will need to use them in paragraph 'Password

2. Password:
java -cp lib\jetty-util-9.2.3.v20140905.jar org.eclipse.jetty.util.security.Password [<username>] <password>

This command will generate 
OBF:1f361iuj1k8k1tvt1tv91k5m1is31f18
MD5:b427830390bf133f2f34355f184f3a2d
CRYPT:alxIkf/hAq3fE

Create using iShell
===================
Run an iShell enabled application with Jetty interface but no ssl
Check which interface is Jetty:
> ishell iface
** if Jetty interface is first on the list use index=0, second on the list index=1
Generate keystore
> ishell iface get <index> keytool create
** a command window will open and instruct the process of creating keystore
> ishell iface get <index> keytool obfuscate <password>
** this will create an obfuscated password that need to be put in ishell.json
** the output of the format OBF:sf8sa7d68v7

ishell.json
===========
{
...
	interface: [
		{
			class: "ca.redleafsolutions.ishell2.interfaces.http.EmbeddedJettyServer",
			port: 80,
			ssl:{
				port: 443,
				keystore:"<path to keystore>",
				"keystore-password": "<obfuscated keystore password>",	<!-- paste the output of "obfuscate" command. should be of the form "OBF:sf8sa7d68v7" -->
				"keymanager-password": "<obfuscated keymanager password>"	<!-- this is optional -->
			}...
		}
	]
...
}


Using Letencrypt.com CERTBOT
============================
Make sure /var/www/html/.well-known is accessile from filesystem and from webroot
> sudo certbot certonly -a webroot -w /var/www/html -d example.com -d www.example.com
check that cerificates were written
> sudo ls -l /etc/letsencrypt/live/example.com

To generate a strong Diffie-Hellman group
> sudo openssl dhparam -out /etc/ssl/certs/dhparam.pem 2048

Write domain ssl snippet
> sudo nano /etc/nginx/snippets/ssl-example.com.conf
'
ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;
'

Write general SSL parameter snippet files
> sudo nano /etc/nginx/snippets/ssl-params.conf
'
# from https://cipherli.st/
# and https://raymii.org/s/tutorials/Strong_SSL_Security_On_nginx.html

ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
ssl_prefer_server_ciphers on;
ssl_ciphers "EECDH+AESGCM:EDH+AESGCM:AES256+EECDH:AES256+EDH";
ssl_ecdh_curve secp384r1;
ssl_session_cache shared:SSL:10m;
ssl_session_tickets off;
ssl_stapling on;
ssl_stapling_verify on;
resolver 8.8.8.8 8.8.4.4 valid=300s;
resolver_timeout 5s;
# Disable preloading HSTS for now.  You can use the commented out header line that includes
# the "preload" directive if you understand the implications.
#add_header Strict-Transport-Security "max-age=63072000; includeSubdomains; preload";
add_header Strict-Transport-Security "max-age=63072000; includeSubdomains";
add_header X-Frame-Options DENY;
add_header X-Content-Type-Options nosniff;

ssl_dhparam /etc/ssl/certs/dhparam.pem;
'

In site configuration file (/etc/nginx/sites-available/example.com) add the SSL
server {
    listen 80;
    listen 443 ssl;

    server_name example.com www.example.com;
    include snippets/ssl-example.com.conf;
    include snippets/ssl-params.conf;

    . . .