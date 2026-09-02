package dispatch

import (
	"encoding/json"
	"fmt"
	"io"
)

// Job is one JSONL workload item.
type Job struct {
	ID         string `json:"id"`
	Kind       string `json:"kind"`
	Payload    string `json:"payload"`
	DurationMS int    `json:"durationMs"`
}

// DecodeJobs reads a stream of JSON objects. It intentionally rejects comments.
func DecodeJobs(r io.Reader) ([]Job, error) {
	decoder := json.NewDecoder(r)
	var jobs []Job
	for line := 1; ; line++ {
		var job Job
		if err := decoder.Decode(&job); err != nil {
			if err == io.EOF {
				return jobs, nil
			}
			return nil, fmt.Errorf("decode job near record %d: %w", line, err)
		}
		if job.ID == "" || job.Kind == "" {
			return nil, fmt.Errorf("job record %d requires id and kind", line)
		}
		jobs = append(jobs, job)
	}
}
