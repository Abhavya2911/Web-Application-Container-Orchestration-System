/*
 * OS Scheduling Engine (C)
 * Algorithms : FCFS, RR (Round Robin)
 * Also does  : resource allocation (CPU + memory) of workloads onto worker nodes.
 *
 * Input (stdin):
 *   <ALGO> <QUANTUM>                      e.g. "RR 2"  or "FCFS 1"
 *   <nodeCount>
 *   <nodeId> <freeCpu> <freeMemMb>        (one line per node)
 *   <workloadCount>
 *   <id> <arrival> <burst> <priority> <cpu> <memMb>   (one line per workload)
 *
 * Output (stdout): one JSON object (see print_json).
 *
 * Model: each worker node is one "CPU". Workloads are first allocated to nodes
 * (worst-fit on free CPU -> balances load), then each node's queue is scheduled
 * independently with the chosen algorithm.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct { int id; double cpu; int mem; } Node;
typedef struct {
    int id, arrival, burst, priority, mem;
    double cpu;
    int node;       /* index into nodes[], -1 = rejected (no node had room) */
    int remaining, start, completion;
} Work;
typedef struct { int node_id, work_id, start, end; } Seg;

static Node *nodes; static int nn;
static Work *works; static int nw;
static Seg *segs;   static int ns = 0;

static int cmp_arrival(const void *a, const void *b) {
    int i = *(const int *)a, j = *(const int *)b;
    if (works[i].arrival != works[j].arrival) return works[i].arrival - works[j].arrival;
    return i - j; /* stable: keep submission order on ties */
}

static void add_seg(int node, int w, int s, int e) {
    /* merge with previous segment if the same workload keeps running */
    if (ns > 0 && segs[ns-1].node_id == nodes[node].id && segs[ns-1].work_id == works[w].id
        && segs[ns-1].end == s) { segs[ns-1].end = e; return; }
    segs[ns].node_id = nodes[node].id; segs[ns].work_id = works[w].id;
    segs[ns].start = s; segs[ns].end = e; ns++;
}

/* ---------- resource allocation ---------- */
static void allocate(void) {
    for (int i = 0; i < nw; i++) {
        int best = -1;
        for (int n = 0; n < nn; n++) {
            if (nodes[n].cpu + 1e-9 >= works[i].cpu && nodes[n].mem >= works[i].mem)
                if (best < 0 || nodes[n].cpu > nodes[best].cpu) best = n;
        }
        works[i].node = best;
        if (best >= 0) { nodes[best].cpu -= works[i].cpu; nodes[best].mem -= works[i].mem; }
    }
}

/* ---------- FCFS ---------- */
static void fcfs(int node, int *q, int m) {
    int t = 0;
    for (int k = 0; k < m; k++) {
        Work *w = &works[q[k]];
        if (t < w->arrival) t = w->arrival;
        w->start = t; t += w->burst; w->completion = t; w->remaining = 0;
        add_seg(node, q[k], w->start, w->completion);
    }
}

/* ---------- Round Robin ---------- */
static void round_robin(int node, int *q, int m, int quantum) {
    long cap = m + 1;
    for (int k = 0; k < m; k++) cap += works[q[k]].burst / quantum + 1;
    int *rq = malloc(sizeof(int) * cap);
    long head = 0, tail = 0; int next = 0, done = 0, t = 0;

    while (done < m) {
        while (next < m && works[q[next]].arrival <= t) rq[tail++] = q[next++];
        if (head == tail) { t = works[q[next]].arrival; continue; } /* CPU idle */
        int p = rq[head++];
        Work *w = &works[p];
        if (w->start < 0) w->start = t;                 /* first time on CPU = response */
        int run = w->remaining < quantum ? w->remaining : quantum;
        add_seg(node, p, t, t + run);
        t += run; w->remaining -= run;
        while (next < m && works[q[next]].arrival <= t) rq[tail++] = q[next++]; /* arrivals first */
        if (w->remaining > 0) rq[tail++] = p;
        else { w->completion = t; done++; }
    }
    free(rq);
}

static void print_json(const char *algo, int quantum) {
    double sw = 0, st = 0, sr = 0; int cnt = 0;
    printf("{\"algorithm\":\"%s\",\"quantum\":%d,\"results\":[", algo, quantum);
    int first = 1;
    for (int i = 0; i < nw; i++) {
        if (works[i].node < 0) continue;
        Work *w = &works[i];
        int tat = w->completion - w->arrival, wait = tat - w->burst, resp = w->start - w->arrival;
        printf("%s{\"id\":%d,\"node\":%d,\"arrival\":%d,\"burst\":%d,\"start\":%d,"
               "\"completion\":%d,\"waiting\":%d,\"turnaround\":%d,\"response\":%d}",
               first ? "" : ",", w->id, nodes[w->node].id, w->arrival, w->burst,
               w->start, w->completion, wait, tat, resp);
        first = 0; sw += wait; st += tat; sr += resp; cnt++;
    }
    printf("],\"timeline\":[");
    for (int i = 0; i < ns; i++)
        printf("%s{\"node\":%d,\"id\":%d,\"start\":%d,\"end\":%d}", i ? "," : "",
               segs[i].node_id, segs[i].work_id, segs[i].start, segs[i].end);
    printf("],\"rejected\":[");
    first = 1;
    for (int i = 0; i < nw; i++)
        if (works[i].node < 0) { printf("%s%d", first ? "" : ",", works[i].id); first = 0; }
    printf("],\"averages\":{\"waiting\":%.2f,\"turnaround\":%.2f,\"response\":%.2f}}\n",
           cnt ? sw / cnt : 0, cnt ? st / cnt : 0, cnt ? sr / cnt : 0);
}

int main(void) {
    char algo[16]; int quantum;
    if (scanf("%15s %d", algo, &quantum) != 2) { fprintf(stderr, "bad header\n"); return 1; }
    if (strcmp(algo, "FCFS") && strcmp(algo, "RR")) { fprintf(stderr, "unknown algorithm %s\n", algo); return 1; }
    if (quantum < 1) quantum = 1;

    if (scanf("%d", &nn) != 1 || nn < 0) return 1;
    nodes = calloc(nn + 1, sizeof(Node));
    for (int i = 0; i < nn; i++)
        if (scanf("%d %lf %d", &nodes[i].id, &nodes[i].cpu, &nodes[i].mem) != 3) return 1;

    if (scanf("%d", &nw) != 1 || nw < 0) return 1;
    works = calloc(nw + 1, sizeof(Work));
    long segCap = nw + 1;
    for (int i = 0; i < nw; i++) {
        Work *w = &works[i];
        if (scanf("%d %d %d %d %lf %d", &w->id, &w->arrival, &w->burst, &w->priority, &w->cpu, &w->mem) != 6) return 1;
        if (w->burst < 1) w->burst = 1;
        w->remaining = w->burst; w->start = -1; w->completion = 0;
        segCap += w->burst / quantum + 1;
    }
    segs = malloc(sizeof(Seg) * segCap);

    allocate();

    int *q = malloc(sizeof(int) * (nw + 1));
    for (int n = 0; n < nn; n++) {
        int m = 0;
        for (int i = 0; i < nw; i++) if (works[i].node == n) q[m++] = i;
        if (m == 0) continue;
        qsort(q, m, sizeof(int), cmp_arrival);
        if (!strcmp(algo, "FCFS")) fcfs(n, q, m);
        else round_robin(n, q, m, quantum);
    }
    print_json(algo, quantum);
    free(q); free(nodes); free(works); free(segs);
    return 0;
}
