package id.github.valfadeev.rundeck.plugin.vault

import com.bettercloud.vault.Vault
import com.bettercloud.vault.api.Auth
import com.bettercloud.vault.api.Logical
import com.bettercloud.vault.response.LogicalResponse
import com.bettercloud.vault.response.LookupResponse
import com.bettercloud.vault.rest.RestResponse
import io.github.valfadeev.rundeck.plugin.vault.VaultStoragePlugin
import spock.lang.Specification

class VaultStoragePluginSpec extends Specification{

    def "trigger error when vault returns with permission deny"(){
        given:

        Properties properties = ["address":"http://localhost:8200",
                                 "maxRetries":"1",
                                 "retryIntervalMilliseconds":"200",
                                 "engineVersion":"2",
                                 "openTimeout":"20",
                                 "readTimeout":"20",
                                 "authBackend":"token",
                                 "token":"123456"]
        def plugin = new VaultStoragePlugin()
        plugin.properties=properties;

        Logical vault = Mock(Logical){
            list(_)>>Mock(LogicalResponse){
                getRestResponse()>>Mock(RestResponse){
                    getStatus()>> 403
                }
            }
        }

        Vault vaultClient = Mock(Vault){
            auth()>>Mock(Auth){
                lookupSelf()>>Mock(LookupResponse){
                    getTTL()>>120
                }
            }
        }

        plugin.vault = vault
        plugin.vaultClient = vaultClient

        when:
        def result = plugin.hasDirectory("keys/test")

        then:
        thrown Exception

    }


    def "has directories keys"(){
        given:

        Properties properties = ["address":"http://localhost:8200",
                                 "maxRetries":"1",
                                 "retryIntervalMilliseconds":"200",
                                 "engineVersion":"2",
                                 "openTimeout":"20",
                                 "readTimeout":"20",
                                 "authBackend":"token",
                                 "token":"123456"]
        def plugin = new VaultStoragePlugin()
        plugin.properties=properties;

        Logical vault = Mock(Logical){
            list(_)>>Mock(LogicalResponse){
                getRestResponse()>>Mock(RestResponse){
                    getStatus()>> 200

                }
                getListData()>>["key1","key2"]
            }
        }

        Vault vaultClient = Mock(Vault){
            auth()>>Mock(Auth){
                lookupSelf()>>Mock(LookupResponse){
                    getTTL()>>120
                }
            }
        }

        plugin.vault = vault
        plugin.vaultClient = vaultClient

        when:
        def result = plugin.hasDirectory("keys/test")

        then:
        result

    }
}
