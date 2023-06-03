package com.spring.batch.jpa.multi.batchconfig;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.spring.batch.jpa.multi.entity.Customer;
import com.spring.batch.jpa.multi.partition.RowPartitioner;
import com.spring.batch.jpa.multi.repository.Customer_Repository;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
	
	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	private Customer_Repository customer_Repository;
	
	
	@Bean
	public FlatFileItemReader<Customer> customerReader(){
		FlatFileItemReader<Customer> flatFileItemReader = new FlatFileItemReader<Customer>();
		flatFileItemReader.setName("Customer-Input-Reader");
		flatFileItemReader.setResource(new ClassPathResource("customers.csv"));
		flatFileItemReader.setLinesToSkip(1);
		flatFileItemReader.setLineMapper(lineMapper());
		
		
		return flatFileItemReader;
		
	} 
	
	public LineMapper<Customer> lineMapper(){
		
		DefaultLineMapper<Customer> defaultLineMapper = new DefaultLineMapper<Customer>();

		DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
		delimitedLineTokenizer.setDelimiter(DelimitedLineTokenizer.DELIMITER_COMMA);
		delimitedLineTokenizer.setStrict(false);
		delimitedLineTokenizer.setNames("id","firstName","lastName","email","gender","contactNo","country","dob");
		
		BeanWrapperFieldSetMapper<Customer> beanWrapperFieldSetMapper = new BeanWrapperFieldSetMapper<>();
		beanWrapperFieldSetMapper.setTargetType(Customer.class);
		
		defaultLineMapper.setLineTokenizer(delimitedLineTokenizer);
		defaultLineMapper.setFieldSetMapper(beanWrapperFieldSetMapper);
		
		return defaultLineMapper;
	}
	
	
	@Bean
	public ItemProcessor<Customer, Customer> processor(){
		return customer->customer;
	}

	
//	@Bean
//	public RepositoryItemWriter<Customer> repositoryItemWriter(){
//		
//		RepositoryItemWriter<Customer> repositoryItemWriter = new RepositoryItemWriter<Customer>();
//		
//		repositoryItemWriter.setRepository(customer_Repository);
//		repositoryItemWriter.setMethodName("save");
//		
//		return repositoryItemWriter;
//	}
	
	
	@Bean 
	public Step readAndWriteStep() {
		
		return stepBuilderFactory.get("readAndWriterStep")
				.<Customer,Customer>chunk(100)
				.reader(customerReader())
				.processor(processor())
				//.writer(repositoryItemWriter())
				.writer((entities)->{
					System.out.println(Thread.currentThread().getName()+" is Executing.......");
					customer_Repository.saveAll(entities);
					})
				//.taskExecutor(taskExecutor())
				.build();
		
	}
	
	@Bean
	public Job primaryJob() {
		return jobBuilderFactory.get("primaryJob")
				.flow(masterStep()).end().build();
	}
	
	
	@Bean
	public TaskExecutor taskExecutor() {
		
//		SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
//		asyncTaskExecutor.setConcurrencyLimit(10);
//		return asyncTaskExecutor;
		ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setMaxPoolSize(12);
		threadPoolTaskExecutor.setCorePoolSize(12);
		threadPoolTaskExecutor.setQueueCapacity(12);
		
		return threadPoolTaskExecutor;
	} 
	
	@Bean
	public RowPartitioner rowPartitioner() {
		return new RowPartitioner(); 
	}
	
	@Bean
	public PartitionHandler partitionHandler() {
		
		TaskExecutorPartitionHandler taskExecutorPartitionHandler = new TaskExecutorPartitionHandler();
		taskExecutorPartitionHandler.setGridSize(10);
		taskExecutorPartitionHandler.setTaskExecutor(taskExecutor());
		taskExecutorPartitionHandler.setStep(readAndWriteStep());
		
		return taskExecutorPartitionHandler;
	}
	
	@Bean
	public Step masterStep() {
		
		return stepBuilderFactory.get("Master-Step")
				.partitioner(readAndWriteStep().getName(),rowPartitioner())
				.partitionHandler(partitionHandler())
				.build();
		
	}
}
