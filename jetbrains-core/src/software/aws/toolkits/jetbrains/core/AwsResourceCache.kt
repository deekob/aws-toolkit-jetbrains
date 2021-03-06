// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import software.amazon.awssdk.services.lambda.LambdaClient
import software.aws.toolkits.core.ToolkitClientManager
import software.aws.toolkits.jetbrains.core.credentials.ProjectAccountSettingsManager
import software.aws.toolkits.jetbrains.services.lambda.LambdaFunction
import software.aws.toolkits.jetbrains.services.lambda.toDataClass
import java.util.concurrent.ConcurrentHashMap

// TODO to be replaced with an actual resource implementation

interface AwsResourceCache {
    fun lambdaFunctions(): List<LambdaFunction>

    companion object {
        fun getInstance(project: Project): AwsResourceCache =
            ServiceManager.getService(project, AwsResourceCache::class.java)
    }
}

class DefaultAwsResourceCache(
    private val accountSettingsManager: ProjectAccountSettingsManager,
    private val clientManager: ToolkitClientManager
) : AwsResourceCache {
    private val cache = ConcurrentHashMap<String, Any>()

    @Suppress("UNCHECKED_CAST")
    override fun lambdaFunctions(): List<LambdaFunction> {
        val credentialProvider = try {
            accountSettingsManager.activeCredentialProvider
        } catch (_: Exception) {
            return emptyList()
        }

        val region = accountSettingsManager.activeRegion
        val credentialProviderId = credentialProvider.id

        val resourceKey = "$region:$credentialProviderId:lambdafunctions"
        return cache.computeIfAbsent(resourceKey) { _ ->
            val client = clientManager.getClient<LambdaClient>()

            return@computeIfAbsent client.listFunctionsPaginator().functions()
                .map { it.toDataClass(credentialProviderId, region) }
                .toList()
        } as List<LambdaFunction>
    }
}
