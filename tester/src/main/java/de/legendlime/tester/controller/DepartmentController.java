package de.legendlime.tester.controller;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.legendlime.tester.config.logging.ResponseLoggingFilter;
import de.legendlime.tester.domain.Department;
import de.legendlime.tester.domain.DepartmentDTO;
import de.legendlime.tester.messaging.AuditRecord;
import de.legendlime.tester.messaging.AuditSourceBean;
import de.legendlime.tester.repository.DepartmentRepository;
import io.micrometer.core.annotation.Timed;

@RestController
@RequestMapping(value = "v1")
@Timed
public class DepartmentController {
	
	private static final Logger LOG = LoggerFactory.getLogger(DepartmentController.class);
	private static final String NOT_FOUND = "Department not found, ID: ";
	private static final String NOT_NULL = "Department cannot be null";

	@Autowired
	private DepartmentRepository repo;
	
	@Autowired 
	VaultTemplate vaultTemplate;
	
	@Autowired
	DataSourceProperties dsProperties;
	
	@Autowired
	AuditSourceBean audit;

	@GetMapping(value = "/departments", 
			    produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Department> getAll(HttpServletRequest request, HttpServletResponse response) {

		LOG.debug("request GET /departments");
		audit.publishAuditMessage(auditHelper("GET", null, request, response));
		return repo.findAll();
	}

	@GetMapping(value = "/departments/{id}", 
			    produces = MediaType.APPLICATION_JSON_VALUE)
	public Department getSingle(@PathVariable(name = "id", required = true) Long id, 
			HttpServletRequest request, HttpServletResponse response) {

		Department dept = repo.findById(id).orElseThrow(() -> {
		   LOG.error(NOT_FOUND, id);
		   return new ResourceNotFoundException(NOT_FOUND + id);
		});
		LOG.debug("request GET /departments/{}", id);
		
		audit.publishAuditMessage(auditHelper("GET", dept, request, response));

		return dept;
	}

	@PostMapping(value = "/departments", 
			     consumes = MediaType.APPLICATION_JSON_VALUE, 
			     produces = MediaType.APPLICATION_JSON_VALUE)
	public Department create(@Valid @RequestBody DepartmentDTO dept, 
			HttpServletRequest request, HttpServletResponse response) {

		if (dept == null) {
			LOG.error(NOT_NULL);
			throw new IllegalArgumentException(NOT_NULL);
		}
		
		// Use DTO to avoid security vulnerability 
		// Persistent entities should not be used as arguments of "@RequestMapping" methods
		Department persistentDept = new Department();
		persistentDept.setDeptId(dept.getDeptId());
		persistentDept.setName(dept.getName());
		persistentDept.setDescription(dept.getDescription());
		LOG.debug("request POST /departments, body: ", persistentDept.toString());
		
		audit.publishAuditMessage(auditHelper("CREATE", persistentDept, request, response));

		return repo.save(persistentDept);
	}

	@PutMapping(value = "/departments/{id}", 
			    consumes = MediaType.APPLICATION_JSON_VALUE, 
			    produces = MediaType.APPLICATION_JSON_VALUE)
	public Department update(@Valid @RequestBody DepartmentDTO dept, 
			                 @PathVariable(name = "id", required = true) Long id, 
			                 HttpServletRequest request, HttpServletResponse response) {
		
		Optional<Department> deptOpt = repo.findById(id);
		if (!deptOpt.isPresent()) {
			LOG.error(NOT_FOUND, id);
			throw new ResourceNotFoundException(NOT_FOUND + id);
		}
		Department d = deptOpt.get();
		d.setDeptId(dept.getDeptId());
		d.setName(dept.getName());
		d.setDescription(dept.getDescription());
		LOG.debug("request PUT /departments {}, body:{}", id, d.toString());

		audit.publishAuditMessage(auditHelper("UPDATE", d, request, response));

		return repo.save(d);
	}
	
	@DeleteMapping(value = "/departments/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<?> delete(@PathVariable(name = "id", required = true) Long id, 
			HttpServletRequest request, HttpServletResponse response) {

		Optional<Department> deptOpt = repo.findById(id);
		if (!deptOpt.isPresent()) {
			LOG.error(NOT_FOUND, id);
			throw new ResourceNotFoundException(NOT_FOUND + id);
		}

		repo.delete(deptOpt.get());
		LOG.debug("request DELETE /departments/{}", id);
		audit.publishAuditMessage(auditHelper("DELETE", deptOpt.get(), request, response));

		return ResponseEntity.ok().build();
	}

	/*--------------------------------*
	 * Auditing methods               *
	 *--------------------------------*/

	private AuditRecord auditHelper(String method, Department obj, 
			HttpServletRequest request, HttpServletResponse response) {
		
		AuditRecord record = new AuditRecord();
		
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		record.setTimestamp(timestamp.toInstant().toString());
		
		record.setNodeName(System.getenv("NODE_NAME"));
		record.setHostName(System.getenv("HOSTNAME"));
		record.setPodName(System.getenv("POD_NAME"));
		
		record.setMethod(method);
		record.setUri(request.getRequestURI());
		record.setClient(request.getRemoteAddr());
		
		String user = request.getRemoteUser();
		if (user != null) {
			record.setUser(user);
		}
		HttpSession session = request.getSession(false);
		if (session != null) {
			record.setSessionId(session.getId());
		}		
		record.setTraceId(response.getHeader(ResponseLoggingFilter.TRACE_ID));
		if (obj != null) {
			record.setObjectType(obj.getClass().getName());
			record.setObjectId(obj.getDeptId());
		}
		if (obj != null && ("CREATE".equalsIgnoreCase(method) || "UPDATE".equalsIgnoreCase(method))) {
			//Creating the ObjectMapper object
		    ObjectMapper mapper = new ObjectMapper();
		    //Converting the Object to JSONString
			try {
				record.setJsonObject(mapper.writeValueAsString(obj));
			} catch (JsonProcessingException e) {
				LOG.error("Error converting audit department object with ID {} to JSON string. Exception {}",
						obj.getDeptId(), e);
			}
		}		
		return record;
	}
}
