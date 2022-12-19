package pl.allegro.tech.servicemesh.envoycontrol.snapshot

import io.envoyproxy.controlplane.cache.SnapshotResources
import io.envoyproxy.envoy.config.cluster.v3.Cluster
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment
import pl.allegro.tech.servicemesh.envoycontrol.groups.Outgoing
import pl.allegro.tech.servicemesh.envoycontrol.groups.TagDependency

data class GlobalSnapshot(
    val clusters: Map<String, Cluster>,
    val allServicesNames: Set<String>,
    val endpoints: Map<String, ClusterLoadAssignment>,
    val clusterConfigurations: Map<String, ClusterConfiguration>,
    val securedClusters: Map<String, Cluster>,
    val tags: Map<String, Set<String>>
) {
    fun <T> getTagsForDependency(
        outgoing: Outgoing,
        mapper: (String, TagDependency) -> T): List<T> {
        val serviceDependencies = outgoing.getServiceDependencies().map { it.service }.toSet()
        return outgoing.getTagDependencies().flatMap { tagDependency ->
            tags.filterKeys { !serviceDependencies.contains(it) }
                .filterValues { it.contains(tagDependency.tag) }
                .map { mapper(it.key, tagDependency) }
        }
    }
}

@Suppress("LongParameterList")
fun globalSnapshot(
    clusters: Iterable<Cluster> = emptyList(),
    endpoints: Iterable<ClusterLoadAssignment> = emptyList(),
    properties: OutgoingPermissionsProperties = OutgoingPermissionsProperties(),
    clusterConfigurations: Map<String, ClusterConfiguration> = emptyMap(),
    securedClusters: List<Cluster> = emptyList(),
    tags: Map<String, Set<String>>
): GlobalSnapshot {
    val clusters = SnapshotResources.create<Cluster>(clusters, "").resources()
    val securedClusters = SnapshotResources.create<Cluster>(securedClusters, "").resources()
    val allServicesNames = getClustersForAllServicesGroups(clusters, properties)
    val endpoints = SnapshotResources.create<ClusterLoadAssignment>(endpoints, "").resources()
    return GlobalSnapshot(
        clusters = clusters,
        securedClusters = securedClusters,
        endpoints = endpoints,
        allServicesNames = allServicesNames,
        clusterConfigurations = clusterConfigurations,
        tags = tags
    )
}

private fun getClustersForAllServicesGroups(
    clusters: Map<String, Cluster>,
    properties: OutgoingPermissionsProperties
): Set<String> {
    val blacklist = properties.allServicesDependencies.notIncludedByPrefix
    if (blacklist.isEmpty()) {
        return clusters.keys
    } else {
        return clusters.filter { (serviceName) -> blacklist.none { serviceName.startsWith(it) } }.keys
    }
}
