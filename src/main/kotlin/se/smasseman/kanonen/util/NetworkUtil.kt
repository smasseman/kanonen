package se.smasseman.kanonen.util

import java.net.InetAddress
import java.net.NetworkInterface
import java.util.regex.Pattern

class NetworkUtil {

    companion object {

        private val IPV4_PATTERN = Pattern.compile("[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}")

        fun getIp(): String {
            return NetworkInterface.networkInterfaces()
                .filter { networkInterface: NetworkInterface ->
                    isNotPoint(
                        networkInterface
                    )
                }
                .flatMap { obj: NetworkInterface -> obj.inetAddresses() }
                .filter { address: InetAddress -> isV4(address) }
                .map { obj: InetAddress -> obj.hostAddress }
                .filter { s: String -> "127.0.0.1" != s }
                .findAny()
                .orElse("localhost")

        }

        private fun isV4(address: InetAddress) =
            IPV4_PATTERN.matcher(address.hostAddress).matches()

        private fun isNotPoint(networkInterface: NetworkInterface) =
            !networkInterface.isPointToPoint
    }
}