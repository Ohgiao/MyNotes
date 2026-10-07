import { createRouter, createWebHistory } from 'vue-router'
import QuickCapture from '@/views/QuickCapture.vue'
import TimelineView from '@/views/TimelineView.vue'
import TopicView from '@/views/TopicView.vue'
import ReviewView from '@/views/ReviewView.vue'
import SearchView from '@/views/SearchView.vue'
import AskView from '@/views/AskView.vue'
import TrashView from '@/views/TrashView.vue'
import EntryDetailView from '@/views/EntryDetailView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'capture', component: QuickCapture },
    { path: '/timeline', name: 'timeline', component: TimelineView },
    { path: '/topic', name: 'topic', component: TopicView },
    { path: '/review', name: 'review', component: ReviewView },
    { path: '/search', name: 'search', component: SearchView },
    { path: '/ask', name: 'ask', component: AskView },
    { path: '/trash', name: 'trash', component: TrashView },
    { path: '/entry/:id', name: 'entry', component: EntryDetailView, props: true }
  ]
})
