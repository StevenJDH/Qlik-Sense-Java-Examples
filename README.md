# Qlik Sense Java Examples

![GitHub release (latest by date including pre-releases)](https://img.shields.io/github/v/release/StevenJDH/Qlik-Sense-Java-Examples?include_prereleases)
![GitHub](https://img.shields.io/github/license/StevenJDH/Qlik-Sense-Java-Examples)

A growing collection of Java-based examples that show how to connect to various Qlik Sense services. I decided to create these because there weren't that many examples available on the web, and the ones that were, used outdated code and ignored resource management and secure programming practices. My goal is to provide a bit more modern code that can be adapted more easily to custom solutions while providing a better starting point for all skill levels. However, software architectural concepts and SOLID principles have been mostly ignored to keep things simple. I will provide more examples as my time permits and prioritize those that are specifically requested.

## Available examples (All projects have a GUI demo for testing)
* _Ticket API_ - Shows how to request a Ticket from the Qlik Proxy Service (QPS) using standard certificates exported from Qlik Sense without needing to convert them to Java KeyStore (*.jks) certificates. Since this example uses REST, it can be easily adapted for other REST-based services, for example, calling the Qlik Repository Service (QRS).
* _Engine API_ - Shows how to create a WebSocket client that uses standard certificates to communicate with the Qlik Engine using JSON-RPC.

## Compatibility
* Java JDK/JRE 12+ is required, but can be adapted for Java 9+.
* IDEs that support Maven projects. Apache NetBeans 11+ is recommend due to its native support.

## Disclaimer
Qlik Sense Java Examples is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

## Do you have any questions?
Many commonly asked questions are answered in the FAQ:
[https://github.com/StevenJDH/Qlik-Sense-Java-Examples/wiki/FAQ](https://github.com/StevenJDH/Qlik-Sense-Java-Examples/wiki/FAQ)

## Want to show your support?

|Method          | Address                                                                                   |
|---------------:|:------------------------------------------------------------------------------------------|
|PayPal:         | [https://www.paypal.me/stevenjdh](https://www.paypal.me/stevenjdh "Steven's Paypal Page") |
|Cryptocurrency: | [Supported options](https://github.com/StevenJDH/StevenJDH/wiki/Donate-Cryptocurrency)    |


// Steven Jenkins De Haro ("StevenJDH" on GitHub)
