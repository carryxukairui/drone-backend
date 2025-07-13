package com.demo.dronebackend.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用于处理alarm表中的trajectory字段
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class MapListTypeHandler implements TypeHandler<List<Map<Object, Object>>> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void setParameter(PreparedStatement ps, int i, List<Map<Object, Object>> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, mapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Map<Object, Object>> getResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<Map<Object, Object>> getResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<Map<Object, Object>> getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<Map<Object, Object>> parse(String json) throws SQLException {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return mapper.readValue(json, new TypeReference<List<Map<Object, Object>>>() {});
        } catch (IOException e) {
            throw new SQLException("Failed to parse JSON string to List<Map>", e);
        }
    }
}
