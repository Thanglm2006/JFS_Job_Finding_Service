package com.example.JFS.repository;

import com.example.JFS.models.Job;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Repository
public class JobRepos implements JpaRepository<Job, Long> {

    @Override
    public void flush() {

    }

    @Override
    public <S extends Job> List<S> saveAllAndFlush(Iterable<S> entities) {
        return List.of();
    }

    @Override
    public void deleteAllInBatch(Iterable<Job> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> longs) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public Job getOne(Long aLong) {
        return null;
    }

    @Override
    public Job getById(Long aLong) {
        return null;
    }

    @Override
    public Job getReferenceById(Long aLong) {
        return null;
    }

    @Override
    public <S extends Job> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends Job> List<S> findAll(Example<S> example) {
        return List.of();
    }

    @Override
    public <S extends Job> List<S> findAll(Example<S> example, Sort sort) {
        return List.of();
    }

    @Override
    public <S extends Job> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends Job> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends Job> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends Job, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public <S extends Job> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public <S extends Job> S save(S entity) {
        return null;
    }

    @Override
    public <S extends Job> List<S> saveAll(Iterable<S> entities) {
        return List.of();
    }

    @Override
    public Optional<Job> findById(Long aLong) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(Long aLong) {
        return false;
    }

    @Override
    public List<Job> findAll() {
        return List.of();
    }

    @Override
    public List<Job> findAllById(Iterable<Long> longs) {
        return List.of();
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(Long aLong) {

    }

    @Override
    public void delete(Job entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends Long> longs) {

    }

    @Override
    public void deleteAll(Iterable<? extends Job> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public List<Job> findAll(Sort sort) {
        return List.of();
    }

    @Override
    public Page<Job> findAll(Pageable pageable) {
        return null;
    }
}
