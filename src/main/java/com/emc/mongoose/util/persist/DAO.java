package com.emc.mongoose.util.persist;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;


/**
 * Created by olga on 27.02.15.
 */
public class DAO {

	public DAO(){
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("openjpa");
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		//
		Mode mode = new Mode("test");
		//
		CriteriaBuilder queryBuilder = em.getCriteriaBuilder();
		CriteriaQuery qdef = queryBuilder.createQuery();
		Root<Mode> modeRoot = qdef.from(Mode.class);
		qdef.select(modeRoot);
		Predicate pGtAge = queryBuilder.equal(modeRoot.get("name"), "test");
		qdef.where(pGtAge);
		Query qry = em.createQuery(qdef); //Step 6
		List<Mode> results = qry.getResultList();
		System.out.println(results.get(0).getName() +" : "+ results.get(0).getId());
		//
		em.getTransaction().commit();
		em.close();
		emf.close();
	}
}
