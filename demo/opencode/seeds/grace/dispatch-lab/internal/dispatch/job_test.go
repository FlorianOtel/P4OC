package dispatch

import (
	"strings"
	"testing"
)

func TestDecodeJobs(t *testing.T) {
	jobs, err := DecodeJobs(strings.NewReader("{\"id\":\"one\",\"kind\":\"uppercase\",\"payload\":\"ok\"}\n"))
	if err != nil {
		t.Fatal(err)
	}
	if len(jobs) != 1 || jobs[0].ID != "one" {
		t.Fatalf("jobs = %#v", jobs)
	}
}

func TestDecodeJobsRejectsMissingIdentity(t *testing.T) {
	_, err := DecodeJobs(strings.NewReader("{\"kind\":\"sum\"}\n"))
	if err == nil || !strings.Contains(err.Error(), "requires id and kind") {
		t.Fatalf("error = %v", err)
	}
}
