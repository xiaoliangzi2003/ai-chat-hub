package com.quantum.ai.chataihub.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quantum.ai.chataihub.entity.sys.SysUser;
import com.quantum.ai.chataihub.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 用户服务（系统级）
 *
 * @author xuhaodong
 * @date 2026/4/15 17:56
 */
@Service
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    public SysUser getByEmail(String email) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getEmail, email);
        return this.getOne(wrapper);
    }

    public void incrementErrorCount(Long userId) {
        SysUser user = this.getById(userId);
        if (user != null) {
            user.setErrorCount(user.getErrorCount() + 1);
            this.updateById(user);
        }
    }

    public void resetErrorCount(Long userId) {
        SysUser user = this.getById(userId);
        if (user != null) {
            user.setErrorCount(0);
            user.setStatus(0);
            user.setLockTime(null);
            this.updateById(user);
        }
    }

    public void lockAccount(Long userId) {
        SysUser user = this.getById(userId);
        if (user != null) {
            user.setStatus(1);
            user.setLockTime(new Date());
            this.updateById(user);
        }
    }

    public boolean isAccountLocked(SysUser user) {
        if (user.getStatus() == 1) {
            // 检查锁定是否已过期
            long lockDuration = 3600 * 1000L; // 1 hour
            if (user.getLockTime() != null &&
                    System.currentTimeMillis() - user.getLockTime().getTime() > lockDuration) {
                // 自动解锁
                resetErrorCount(user.getId());
                return false;
            }
            return true;
        }
        return false;
    }
}
