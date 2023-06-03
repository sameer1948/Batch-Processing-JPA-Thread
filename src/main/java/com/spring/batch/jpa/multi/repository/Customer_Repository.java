package com.spring.batch.jpa.multi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.batch.jpa.multi.entity.Customer;

public interface Customer_Repository extends JpaRepository<Customer, Integer>{

}
