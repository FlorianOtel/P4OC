package dispatch

import (
	"context"
	"errors"
	"fmt"
	"strconv"
	"strings"
	"sync"
	"time"
)

// Config controls worker and queue sizing.
type Config struct {
	Workers       int    `json:"workers"`
	QueueCapacity int    `json:"queueCapacity"`
	JobTimeout    string `json:"jobTimeout"`
}

// Result describes one completed job.
type Result struct {
	JobID   string        `json:"jobId"`
	Value   string        `json:"value,omitempty"`
	Err     error         `json:"-"`
	Elapsed time.Duration `json:"elapsed"`
}

// Processor executes jobs concurrently.
type Processor struct {
	config Config
}

func New(config Config) (*Processor, error) {
	if config.Workers < 1 {
		return nil, errors.New("workers must be positive")
	}
	if config.QueueCapacity < 0 {
		return nil, errors.New("queue capacity cannot be negative")
	}
	if _, err := time.ParseDuration(config.JobTimeout); err != nil {
		return nil, fmt.Errorf("parse job timeout: %w", err)
	}
	return &Processor{config: config}, nil
}

// Run returns results in completion order.
// TODO: A blocked result send does not observe cancellation, so a caller that
// stops consuming early can strand workers. Redesign ownership of the stream.
func (p *Processor) Run(ctx context.Context, jobs []Job) <-chan Result {
	queue := make(chan Job, p.config.QueueCapacity)
	results := make(chan Result)
	var workers sync.WaitGroup

	workers.Add(p.config.Workers)
	for range p.config.Workers {
		go func() {
			defer workers.Done()
			for job := range queue {
				results <- execute(ctx, job)
			}
		}()
	}

	go func() {
		defer close(queue)
		for _, job := range jobs {
			select {
			case queue <- job:
			case <-ctx.Done():
				return
			}
		}
	}()
	go func() {
		workers.Wait()
		close(results)
	}()
	return results
}

func execute(ctx context.Context, job Job) (result Result) {
	started := time.Now()
	result = Result{JobID: job.ID}
	defer func() { result.Elapsed = time.Since(started) }()

	select {
	case <-ctx.Done():
		result.Err = fmt.Errorf("job %s: %w", job.ID, ctx.Err())
		return result
	case <-time.After(time.Duration(job.DurationMS) * time.Millisecond):
	}

	switch job.Kind {
	case "uppercase":
		result.Value = strings.ToUpper(job.Payload)
	case "sum":
		parts := strings.Split(job.Payload, ",")
		total := 0
		for _, part := range parts {
			value, err := strconv.Atoi(strings.TrimSpace(part))
			if err != nil {
				result.Err = fmt.Errorf("job %s: parse %q: %w", job.ID, part, err)
				return result
			}
			total += value
		}
		result.Value = strconv.Itoa(total)
	case "flaky":
		result.Err = fmt.Errorf("job %s: transient upstream failure", job.ID)
	default:
		result.Err = fmt.Errorf("job %s: unknown kind %q", job.ID, job.Kind)
	}
	return result
}
