package no.nav.fo.veilarbsituasjon.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
@Transactional
public abstract class AbstractDAO<T> {

    private SessionFactory sessionFactory;

    public AbstractDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected Session getSession() {
        Session session = sessionFactory.getCurrentSession();
        return session;
    }

    protected abstract Logger log();

    public <U extends T> U saveOrUpdate(U u) {
        getSession().saveOrUpdate(u);
        return u;
    }

    public <U extends T> U save(U u) {
        getSession().save(u);
        return u;
    }

    @SuppressWarnings("unchecked")
    public <U extends T> U get(Class<U> clazz, Long id) {
        return (U) getSession().get(clazz, id);
    }

    @SuppressWarnings("unchecked")
    public <U extends T> U get(Class<U> clazz, String id) {
        return (U) getSession().get(clazz, id);
    }

    public <U extends T> void delete(U u) {
        getSession().delete(u);
    }

    @SuppressWarnings("unchecked")
    public <U extends T> List<U> findAll(Class<U> clazz) {
        return sessionFactory.getCurrentSession().createCriteria(clazz).list();
    }

}
