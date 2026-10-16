package com.teamelite.orchestrator.model;

/** A worker node plus how much of it is currently reserved by scheduled/running workloads. */
public record NodeView(Long id, String name, double cpuCores, int memoryMb, NodeStatus status,
                       double usedCpu, int usedMemory, int workloadCount) {}
