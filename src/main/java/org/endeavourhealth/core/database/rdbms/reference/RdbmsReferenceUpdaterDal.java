package org.endeavourhealth.core.database.rdbms.reference;

import org.endeavourhealth.core.database.dal.reference.ReferenceUpdaterDalI;
import org.endeavourhealth.core.database.dal.reference.models.DeprivationLookup;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.core.database.rdbms.reference.models.*;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class RdbmsReferenceUpdaterDal implements ReferenceUpdaterDalI {


    @Override
    public void updateLosaMap(String lsoaCode, String lsoaName) throws Exception {

        EntityManager entityManager = ConnectionManager.getReferenceEntityManager();
        try {
            String sql = "select r"
                    + " from RdbmsLsoaLookup r"
                    + " where r.lsoaCode = :lsoaCode";

            Query query = entityManager
                    .createQuery(sql, RdbmsLsoaLookup.class)
                    .setParameter("lsoaCode", lsoaCode);

            RdbmsLsoaLookup lookup = null;
            try {
                lookup = (RdbmsLsoaLookup) query.getSingleResult();
            } catch (NoResultException e) {
                lookup = new RdbmsLsoaLookup();
                lookup.setLsoaCode(lsoaCode);
            }

            lookup.setLsoaName(lsoaName);

            entityManager.getTransaction().begin();
            entityManager.persist(lookup);
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            entityManager.close();
        }
    }

    @Override
    public void updateMosaMap(String msoaCode, String msoaName) throws Exception {

        EntityManager entityManager = ConnectionManager.getReferenceEntityManager();
        try {
            String sql = "select r"
                    + " from RdbmsMsoaLookup r"
                    + " where r.msoaCode = :msoaCode";


            Query query = entityManager
                    .createQuery(sql, RdbmsMsoaLookup.class)
                    .setParameter("msoaCode", msoaCode);

            RdbmsMsoaLookup lookup = null;
            try {
                lookup = (RdbmsMsoaLookup) query.getSingleResult();
            } catch (NoResultException e) {
                lookup = new RdbmsMsoaLookup();
                lookup.setMsoaCode(msoaCode);
            }

            lookup.setMsoaName(msoaName);

            entityManager.getTransaction().begin();
            entityManager.persist(lookup);
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            entityManager.close();
        }
    }


    @Override
    public void updateCcgMap(String ccgCode, String ccgName) throws Exception {

        EntityManager entityManager = ConnectionManager.getReferenceEntityManager();
        try {
            String sql = "select r"
                    + " from RdbmsCcgLookup r"
                    + " where r.code = :ccgCode";


            Query query = entityManager
                    .createQuery(sql, RdbmsCcgLookup.class)
                    .setParameter("ccgCode", ccgCode);

            RdbmsCcgLookup lookup = null;
            try {
                lookup = (RdbmsCcgLookup) query.getSingleResult();
            } catch (NoResultException e) {
                lookup = new RdbmsCcgLookup();
                lookup.setCode(ccgCode);
            }

            lookup.setName(ccgName);

            entityManager.getTransaction().begin();
            entityManager.persist(lookup);
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            entityManager.close();
        }
    }

    @Override
    public void updateWardMap(String wardCode, String wardName) throws Exception {
        EntityManager entityManager = ConnectionManager.getReferenceEntityManager();
        try {
            String sql = "select r"
                    + " from RdbmsWardLookup r"
                    + " where r.code = :wardCode";


            Query query = entityManager
                    .createQuery(sql, RdbmsWardLookup.class)
                    .setParameter("wardCode", wardCode);

            RdbmsWardLookup lookup = null;
            try {
                lookup = (RdbmsWardLookup) query.getSingleResult();
            } catch (NoResultException e) {
                lookup = new RdbmsWardLookup();
                lookup.setCode(wardCode);
            }

            lookup.setName(wardName);

            entityManager.getTransaction().begin();
            entityManager.persist(lookup);
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            entityManager.close();
        }
    }

    @Override
    public void updateLocalAuthorityMap(String localAuthorityCode, String localAuthorityName) throws Exception {
        EntityManager entityManager = ConnectionManager.getReferenceEntityManager();
        try {
            String sql = "select r"
                    + " from RdbmsLocalAuthorityLookup r"
                    + " where r.code = :localAuthorityCode";


            Query query = entityManager
                    .createQuery(sql, RdbmsLocalAuthorityLookup.class)
                    .setParameter("localAuthorityCode", localAuthorityCode);

            RdbmsLocalAuthorityLookup lookup = null;
            try {
                lookup = (RdbmsLocalAuthorityLookup) query.getSingleResult();
            } catch (NoResultException e) {
                lookup = new RdbmsLocalAuthorityLookup();
                lookup.setCode(localAuthorityCode);
            }

            lookup.setName(localAuthorityName);

            entityManager.getTransaction().begin();
            entityManager.persist(lookup);
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            entityManager.close();
        }
    }



    @Override
    public void updateDeprivationMap(DeprivationLookup proxy) throws Exception {

        EntityManager entityManager = ConnectionManager.getReferenceEntityManager();

        String lsoaCode = proxy.getLsoaCode();

        try {
            String sql = "select r"
                    + " from RdbmsDeprivationLookup r"
                    + " where r.lsoaCode = :lsoaCode";

            Query query = entityManager
                    .createQuery(sql, RdbmsDeprivationLookup.class)
                    .setParameter("lsoaCode", lsoaCode);

            RdbmsDeprivationLookup lookup = null;
            try {
                lookup = (RdbmsDeprivationLookup) query.getSingleResult();
            } catch (NoResultException e) {
                lookup = new RdbmsDeprivationLookup();
                lookup.setLsoaCode(lsoaCode);
            }

            lookup.setImdScore(proxy.getImdScore());
            lookup.setImdRank(proxy.getImdRank());
            lookup.setImdDecile(proxy.getImdDecile());
            lookup.setIncomeScore(proxy.getIncomeScore());
            lookup.setIncomeRank(proxy.getIncomeRank());
            lookup.setIncomeDecile(proxy.getIncomeDecile());
            lookup.setEmploymentScore(proxy.getEmploymentScore());
            lookup.setEmploymentRank(proxy.getEmploymentRank());
            lookup.setEmploymentDecile(proxy.getEmploymentDecile());
            lookup.setEducationScore(proxy.getEducationScore());
            lookup.setEducationRank(proxy.getEducationRank());
            lookup.setEducationDecile(proxy.getEducationDecile());
            lookup.setHealthScore(proxy.getHealthScore());
            lookup.setHealthRank(proxy.getHealthRank());
            lookup.setHealthDecile(proxy.getHealthDecile());
            lookup.setCrimeScore(proxy.getCrimeScore());
            lookup.setCrimeRank(proxy.getCrimeRank());
            lookup.setCrimeDecile(proxy.getCrimeDecile());
            lookup.setHousingAndServicesBarriersScore(proxy.getHousingAndServicesBarriersScore());
            lookup.setHousingAndServicesBarriersRank(proxy.getHousingAndServicesBarriersRank());
            lookup.setHousingAndServicesBarriersDecile(proxy.getHousingAndServicesBarriersDecile());
            lookup.setLivingEnvironmentScore(proxy.getLivingEnvironmentScore());
            lookup.setLivingEnvironmentRank(proxy.getLivingEnvironmentRank());
            lookup.setLivingEnvironmentDecile(proxy.getLivingEnvironmentDecile());
            lookup.setIdaciScore(proxy.getIdaciScore());
            lookup.setIdaciRank(proxy.getIdaciRank());
            lookup.setIdaciDecile(proxy.getIdaciDecile());
            lookup.setIdaopiScore(proxy.getIdaopiScore());
            lookup.setIdaopiRank(proxy.getIdaopiRank());
            lookup.setIdaopiDecile(proxy.getIdaopiDecile());
            lookup.setChildrenAndYoungSubDomainScore(proxy.getChildrenAndYoungSubDomainScore());
            lookup.setChildrenAndYoungSubDomainRank(proxy.getChildrenAndYoungSubDomainRank());
            lookup.setChildrenAndYoungSubDomainDecile(proxy.getChildrenAndYoungSubDomainDecile());
            lookup.setAdultSkillsSubDomainScore(proxy.getAdultSkillsSubDomainScore());
            lookup.setAdultSkillsSubDomainRank(proxy.getAdultSkillsSubDomainRank());
            lookup.setAdultSkillsSubDomainDecile(proxy.getAdultSkillsSubDomainDecile());
            lookup.setGeographicalBarriersSubDomainScore(proxy.getGeographicalBarriersSubDomainScore());
            lookup.setGeographicalBarriersSubDomainRank(proxy.getGeographicalBarriersSubDomainRank());
            lookup.setGeographicalBarriersSubDomainDecile(proxy.getGeographicalBarriersSubDomainDecile());
            lookup.setWiderBarriersSubDomainScore(proxy.getWiderBarriersSubDomainScore());
            lookup.setWiderBarriersSubDomainRank(proxy.getWiderBarriersSubDomainRank());
            lookup.setWiderBarriersSubDomainDecile(proxy.getWiderBarriersSubDomainDecile());
            lookup.setIndoorsSubDomainScore(proxy.getIndoorsSubDomainScore());
            lookup.setIndoorsSubDomainRank(proxy.getIndoorsSubDomainRank());
            lookup.setIndoorsSubDomainDecile(proxy.getIndoorsSubDomainDecile());
            lookup.setOutdoorsSubDomainScore(proxy.getOutdoorsSubDomainScore());
            lookup.setOutdoorsSubDomainRank(proxy.getOutdoorsSubDomainRank());
            lookup.setOutdoorsSubDomainDecile(proxy.getOutdoorsSubDomainDecile());
            lookup.setTotalPopulation(proxy.getTotalPopulation());
            lookup.setDependentChildren0To15(proxy.getDependentChildren0To15());
            lookup.setPopulation16To59(proxy.getPopulation16To59());
            lookup.setOlderPopulation60AndOver(proxy.getOlderPopulation60AndOver());
            //lookup.setWorkingAgePopulation(proxy.getWorkingAgePopulation());

            entityManager.getTransaction().begin();
            entityManager.persist(lookup);
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            entityManager.close();
        }
    }
}
