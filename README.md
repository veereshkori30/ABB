Cost optimaztion in aws

Reserved Instances (RIs): For workloads that are predictable and run continuously, purchasing Reserved Instances (for EC2, RDS, ElastiCache, etc.) can provide significant savings (up to 75% off the on-demand pricing).
Savings Plans: These are flexible alternatives to Reserved Instances, offering savings in exchange for committing to a consistent amount of usage (measured in $/hour) over one or three years. This applies to various services like EC2, Lambda, and AWS Fargate.

AWS provides auto-scaling services for both compute (EC2 Auto Scaling) and databases (Amazon RDS). By automatically scaling resources based on traffic or demand, you can ensure you’re only paying for the resources you actually need at any given time.
Elastic Load Balancing (ELB) also helps distribute traffic efficiently, ensuring that your infrastructure is optimized for cost and performance.
