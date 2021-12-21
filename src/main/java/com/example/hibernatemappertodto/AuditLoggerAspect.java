package com.example.hibernatemappertodto;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module.Feature;

@Aspect
@Configuration
public class AuditLoggerAspect {

	private Logger logger = LoggerFactory.getLogger(AuditLoggerAspect.class);
	
	@Autowired
	private AuthorRepository authorRepository;
	
	@PersistenceContext
	private EntityManager entityManager;

	@Around(value = "execution(public * save(..)) && this(org.springframework.data.repository.CrudRepository)")
	public Object onSaveExecuted(ProceedingJoinPoint pjp) throws Throwable {
		return fireAudit(pjp, false);
	}

	private Object fireAudit(ProceedingJoinPoint pjp, boolean b) {
		Optional<Class> annotatedInterface = getAnnotatedInterface(pjp);
		if (annotatedInterface.isPresent()) {
			try {
				Author author = (Author)pjp.getArgs()[0];
				Optional<Author> oldAuthorOptional = authorRepository.findById(author.getAuthorId());
				if (oldAuthorOptional.isPresent()) {
					Author oldAuthor = oldAuthorOptional.get();
					Hibernate5Module module = new Hibernate5Module();
					module.enable(Feature.FORCE_LAZY_LOADING);
					ObjectMapper objectMapper = new ObjectMapper();
					objectMapper.registerModule(module);
					Author authorRef = entityManager.getReference(Author.class, 1);
					String json = objectMapper.writeValueAsString(authorRef);
					System.out.println("json = " + json);
					Author authorWithoutEntity = objectMapper.readValue(json.getBytes(), Author.class);
					System.out.println("authorWithoutEntity = " + authorWithoutEntity);
				}
				Object modifiedObject = pjp.proceed(pjp.getArgs());
				//logger.info("Old values=" + ((Author)modifiedObject).getSavedState());
				logger.info("New values=" + modifiedObject);
				return modifiedObject;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Optional<Class> getAnnotatedInterface(JoinPoint pjp) {
		for (Class i : pjp.getTarget().getClass().getInterfaces()) {
			if (i.isAnnotationPresent(AUDITABLE_REPO.class) && CrudRepository.class.isAssignableFrom(i)) {
				return Optional.of(i);
			}
		}
		return Optional.empty();
	}

}
