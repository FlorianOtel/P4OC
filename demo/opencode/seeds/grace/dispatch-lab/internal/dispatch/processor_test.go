package dispatch

import (
	"context"
	"strings"
	"testing"
	"time"
)

func TestProcessorRunsSupportedJobs(t *testing.T) {
	processor, err := New(Config{Workers: 2, QueueCapacity: 1, JobTimeout: "1s"})
	if err != nil {
		t.Fatal(err)
	}
	jobs := []Job{
		{ID: "name", Kind: "uppercase", Payload: "grace", DurationMS: 2},
		{ID: "total", Kind: "sum", Payload: "10, 20,12", DurationMS: 1},
	}

	got := map[string]Result{}
	for result := range processor.Run(context.Background(), jobs) {
		got[result.JobID] = result
	}
	if got["name"].Value != "GRACE" || got["name"].Err != nil {
		t.Fatalf("name result = %#v", got["name"])
	}
	if got["total"].Value != "42" || got["total"].Err != nil {
		t.Fatalf("total result = %#v", got["total"])
	}
}

func TestProcessorReportsJobError(t *testing.T) {
	processor, err := New(Config{Workers: 1, JobTimeout: "1s"})
	if err != nil {
		t.Fatal(err)
	}
	results := processor.Run(context.Background(), []Job{{ID: "bad", Kind: "sum", Payload: "4,nope"}})
	result := <-results
	if result.Err == nil || !strings.Contains(result.Err.Error(), `job bad`) {
		t.Fatalf("error = %v", result.Err)
	}
}

func TestProcessorObservesAlreadyCanceledContext(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	cancel()

	select {
	case result := <-func() <-chan Result {
		completed := make(chan Result, 1)
		go func() { completed <- execute(ctx, Job{ID: "late", Kind: "uppercase", Payload: "x", DurationMS: 100}) }()
		return completed
	}():
		if result.Err == nil {
			t.Fatal("expected cancellation error")
		}
	case <-time.After(250 * time.Millisecond):
		t.Fatal("canceled job did not complete promptly")
	}
}
