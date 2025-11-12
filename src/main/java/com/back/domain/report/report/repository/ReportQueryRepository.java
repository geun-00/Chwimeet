package com.back.domain.report.report.repository;

import com.back.domain.report.report.common.ReportType;
import com.back.domain.report.report.dto.ReportResBody;
import com.back.domain.report.report.entity.Report;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.back.domain.member.member.entity.QMember.member;
import static com.back.domain.report.report.entity.QReport.report;

@Repository
public class ReportQueryRepository extends QuerydslRepositorySupport {

    private final JPAQueryFactory queryFactory;

    public ReportQueryRepository(JPAQueryFactory queryFactory) {
        super(Report.class);
        this.queryFactory = queryFactory;
    }

    public Page<ReportResBody> getReports(Pageable pageable, ReportType reportType) {
        JPAQuery<ReportResBody> query = queryFactory.select(Projections.constructor(
                                                            ReportResBody.class,
                                                            report.id,
                                                            report.reportType,
                                                            report.targetId,
                                                            report.comment,
                                                            member.id,
                                                            report.createdAt
                                                    ))
                                                    .from(report)
                                                    .join(report.member, member)
                                                    .where(reportTypeEq(reportType));

        List<ReportResBody> content = getQuerydsl().applyPagination(pageable, query).fetch();
        JPAQuery<Long> countQuery = queryFactory.select(report.count())
                                                .from(report)
                                                .where(reportTypeEq(reportType));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression reportTypeEq(ReportType reportType) {
        return reportType != null ? report.reportType.eq(reportType) : null;
    }
}
