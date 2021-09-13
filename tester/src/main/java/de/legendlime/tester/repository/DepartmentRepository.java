package de.legendlime.tester.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import de.legendlime.tester.domain.Department;

public interface DepartmentRepository extends CrudRepository<Department, Long> {
	
	public List<Department> findAll();

}
