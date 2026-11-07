package org.endeavourhealth.core.database.rdbms.reference;

import com.google.common.base.Strings;
import org.endeavourhealth.core.database.dal.reference.PostcodeDalI;
import org.endeavourhealth.core.database.dal.reference.models.PostcodeLookup;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RdbmsPostcodeDal implements PostcodeDalI {
    private static final Logger LOG = LoggerFactory.getLogger(RdbmsPostcodeDal.class);

    public PostcodeLookup getPostcodeReference(String postcode) throws Exception {

        //if called with an empty postcode, just return null
        if (Strings.isNullOrEmpty(postcode)) {
            return null;
        }

        //we force everything to upper case when creating the table, so do that now
        postcode = postcode.toUpperCase();

        //because we've got no guarantee how/where the raw postcodes are spaced, we use the string without spaces as our primary key
        postcode = postcode.replaceAll(" ", "");

        Connection connection = ConnectionManager.getReferenceConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT p.postcode_no_space, p.postcode, p.lsoa_code, p.msoa_code, p.ward_code, p.ccg_code, p.local_authority_code,"
                    + " p.lsoa_2001_code, p.lsoa_2011_code, p.msoa_2001_code, p.msoa_2011_code, t.townsend_2011_score"
                    + " FROM postcode_lookup p"
                    + " LEFT OUTER JOIN townsend_deprivation_lookup t"
                    + " ON t.postcode_no_space = p.postcode_no_space"
                    + " WHERE p.postcode_no_space = ?";
            ps = connection.prepareStatement(sql);

            ps.setString(1, postcode);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PostcodeLookup ret = new PostcodeLookup();

                int col = 1;
                ret.setPostcodeNoSpace(rs.getString(col++));
                ret.setPostcode(rs.getString(col++));
                ret.setLsoaCode(rs.getString(col++));
                ret.setMsoaCode(rs.getString(col++));
                ret.setWardCode(rs.getString(col++));
                ret.setCcgCode(rs.getString(col++));
                ret.setLocalAuthorityCode(rs.getString(col++));
                ret.setLsoa2001Code(rs.getString(col++));
                ret.setLsoa2011Code(rs.getString(col++));
                ret.setMsoa2001Code(rs.getString(col++));
                ret.setMsoa2011Code(rs.getString(col++));

                double d = rs.getDouble(col++);
                if (!rs.wasNull()) {
                    ret.setTownsendDeprivationScore(new Double(d));
                }

                return ret;

            } else {
                return null;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }

}
