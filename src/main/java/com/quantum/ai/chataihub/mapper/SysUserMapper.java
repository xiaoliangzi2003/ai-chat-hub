package com.quantum.ai.chataihub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quantum.ai.chataihub.entity.sys.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户核心信息表(SysUser)表数据库访问层(Mapper)
 *
 * @author quantum
 * @since 2026-04-15 17:26:06
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

}
