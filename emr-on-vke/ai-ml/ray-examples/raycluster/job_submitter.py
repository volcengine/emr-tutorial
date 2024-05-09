"""
CLI interface for RayCluster examples.
"""

import os
import sys
import time

from ray.job_submission import JobSubmissionClient
from config import RAY_WEB_UI_BASE_URI, PULL_STATUS_SLEEP_DURATION

class JobSubmitter:
    def __init__(self, client):
        self.client = client

    def submit_job(self, entrypoint, working_dir):
        job_id = self.client.submit_job(
            entrypoint=entrypoint, runtime_env={"working_dir": working_dir}
        )
        return job_id

    def submit_job(self, entrypoint, working_dir, env_vars):
        job_id = self.client.submit_job(
            entrypoint=entrypoint,
            runtime_env={"working_dir": working_dir, "env_vars": env_vars},
        )
        return job_id

class JobDescription:
    def __init__(self, job_name, entrypoint, working_dir="./", env_vars=None):
        self.job_name = job_name
        self.entrypoint = entrypoint
        self.working_dir = working_dir
        self.env_vars = env_vars if env_vars is not None else {}

    def pretty_print(self):
        print("\n")
        print(f"------Job << {self.job_name} >>------")
        print(f"Entrypoint: {self.entrypoint}")
        print(f"Working Directory: {self.working_dir}")
        print(f"Environment Variables: {self.env_vars}")
        print("\n")


class JobMonitor:
    @staticmethod
    def monitor_job(client, job_id):
        while True:
            job_status = client.get_job_status(job_id)
            if job_status == "SUCCEEDED" or job_status == "FAILED":
                print(f"Job {job_id}'s status is: {job_status}")
                if job_status == "FAILED":
                    assert False, "Job status is FAILED"
                break
            elif job_status == "PENDING" or job_status == "RUNNING":
                print(
                    f"Job is {job_status}, will sleep {PULL_STATUS_SLEEP_DURATION} seconds before next check."
                )
                time.sleep(PULL_STATUS_SLEEP_DURATION)

        # 获取作业结果
        job_result = client.get_job_info(job_id)
        print(f"Job {job_id} result: {job_result}")

        # 获取作业日志
        # job_logs = client.get_job_logs(job_id)
        # print(f"Job {job_id} result: {job_logs}")

def run(job_script = None):
    """
    The main function executes on commands:
    `python -m RayCluster-examples` and `$ RayCluster-examples `.

    This is your program's entry point.

    You can change this function to do whatever you want.
    Examples:
        * Run a test suite
        * Run a server
    """
    print("Will submit some jobs to ray cluster.")
    client = JobSubmissionClient(RAY_WEB_UI_BASE_URI)
    submitter = JobSubmitter(client)
    #workdir = "/tmp/ray-workdir-tmp"
    #os.makedirs(workdir, exist_ok=True)

    if job_script:
        job_queue = [
            JobDescription(
                "Run " + job_script,
                "python " + job_script,
            )]
    else:
        script_dir = os.path.dirname(os.path.abspath(__file__))

        job_queue = [
            JobDescription(
                "Test pyarrow can access HDFS on ray cluster.",
                "python data_quick_start.py",
                working_dir = script_dir
            ),
            # JobDescription(
            #     "Test pyarrow can access HDFS via core-site.xml on ray cluster.",
            #     "python demo_pyarrow_hdfs_core_site.py",
            # ),
            # JobDescription(
            #     "Test ray data module can access hdfs via pyarrow on ray cluster.",
            #     "python tune_torch_benchmark.py",
            # ),
            # JobDescription(
            #     "Test ray data module can access hdfs with config file via pyarrow on ray cluster.",
            #     "python demo_ray_data_hdfs_core_site.py",
            # ),
            # JobDescription(
            #     "Test ray data module with some simple code on ray cluster.",
            #     "python demo_ray_data_simple.py",
            #     working_dir='./',
            #     env_vars={},
            # ),
        ]

    for job_desc in job_queue:
        job_desc.pretty_print()
        job_id = submitter.submit_job(
            job_desc.entrypoint, job_desc.working_dir, job_desc.env_vars
        )
        JobMonitor.monitor_job(client, job_id)


if __name__== "__main__" :
    if len(sys.argv) == 1:
        run()
    else:
        run(sys.argv[1])
