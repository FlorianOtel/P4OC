package main

import (
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"os/signal"

	"example.com/dispatch-lab/internal/dispatch"
)

func main() {
	configPath := flag.String("config", "fixtures/config.json", "dispatcher JSON config")
	jobsPath := flag.String("jobs", "fixtures/jobs.jsonl", "JSONL workload")
	flag.Parse()
	if err := run(*configPath, *jobsPath); err != nil {
		fmt.Fprintln(os.Stderr, "dispatch:", err)
		os.Exit(1)
	}
}

func run(configPath, jobsPath string) error {
	configFile, err := os.Open(configPath)
	if err != nil {
		return fmt.Errorf("open config: %w", err)
	}
	defer configFile.Close()
	var config dispatch.Config
	if err := json.NewDecoder(configFile).Decode(&config); err != nil {
		return fmt.Errorf("decode config: %w", err)
	}

	jobsFile, err := os.Open(jobsPath)
	if err != nil {
		return fmt.Errorf("open jobs: %w", err)
	}
	defer jobsFile.Close()
	jobs, err := dispatch.DecodeJobs(jobsFile)
	if err != nil {
		return err
	}
	processor, err := dispatch.New(config)
	if err != nil {
		return err
	}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt)
	defer stop()
	failed := false
	for result := range processor.Run(ctx, jobs) {
		if result.Err != nil {
			failed = true
			fmt.Printf("FAIL %-20s %v\n", result.JobID, result.Err)
		} else {
			fmt.Printf("OK   %-20s %s (%s)\n", result.JobID, result.Value, result.Elapsed)
		}
	}
	if failed {
		return fmt.Errorf("one or more jobs failed")
	}
	return nil
}
